package bg.deck.santaseservice.tabla;

import bg.deck.santaseservice.enums.GameType;
import bg.deck.santaseservice.exception.PlayerInactivitySurrenderException;
import bg.deck.santaseservice.exception.TablaException;
import bg.deck.santaseservice.model.Game;
import bg.deck.santaseservice.model.Player;
import bg.deck.santaseservice.model.TablaGameState;
import bg.deck.santaseservice.model.request.MoveRequest;
import bg.deck.santaseservice.model.response.SearchGameResponse;
import bg.deck.santaseservice.service.GameInactivityService;
import bg.deck.santaseservice.service.GameUtilService;
import bg.deck.santaseservice.service.WebSocketService;
import bg.deck.santaseservice.tabla.engine.BackgammonRules;
import bg.deck.santaseservice.tabla.engine.BoardState;
import bg.deck.santaseservice.tabla.engine.Dice;
import bg.deck.santaseservice.tabla.engine.Hop;
import bg.deck.santaseservice.tabla.engine.Side;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Обикновена табла — the request-facing operations.
 *
 * <p>The turn is applied <em>incrementally</em>: roll, then one hop per request
 * with undo available, then confirm. That matches every existing endpoint's
 * shape, lets the opponent watch checkers move rather than teleport, and keeps
 * the database authoritative so a refresh mid-turn restores exactly.
 */
@Log4j2
@RequiredArgsConstructor
@Service
public class TablaService {

    private static final int MAX_INACTIVITY = 3;

    private final TablaUtilService tablaUtilService;
    private final TablaDiceService diceService;
    private final GameUtilService gameUtilService;
    private final GameInactivityService gameInactivityService;
    private final WebSocketService webSocketService;

    private final Queue<String> matchQueue = new ConcurrentLinkedQueue<>();

    /* ------------------------------------------------------------------
       Matchmaking
       ------------------------------------------------------------------ */

    public void searchGame() {
        String username = gameUtilService.getUsername();

        // Typed, so an in-progress Santase game does not block a табла search.
        if (!gameUtilService.checkIfUserExistsAndIsAvailable(username, GameType.TABLA)) {
            return;
        }

        if (matchQueue.contains(username)) {
            webSocketService.notifyGameSearch(username, GameType.TABLA, SearchGameResponse.waiting());
            return;
        }

        String waiting = matchQueue.poll();

        if (waiting == null) {
            matchQueue.offer(username);
            webSocketService.notifyGameSearch(username, GameType.TABLA, SearchGameResponse.waiting());
            return;
        }

        Player first = gameUtilService.newPlayerFor(waiting);
        Player second = gameUtilService.newPlayerFor(username);

        Game game = tablaUtilService.startGame(first, second);
        log.info("Табла match: {} vs {} ({})", waiting, username, game.getId());

        webSocketService.notifyGameSearch(List.of(waiting, username), GameType.TABLA,
                SearchGameResponse.started(game.getId()));
        gameInactivityService.updateNextMoveTime(game);
    }

    public void cancelSearch(String username) {
        matchQueue.remove(username);
    }

    public void getState() {
        String username = gameUtilService.getUsername();
        Game game = tablaUtilService.findActiveGame(username);
        tablaUtilService.push(game, username);
    }

    /* ------------------------------------------------------------------
       The turn
       ------------------------------------------------------------------ */

    @Transactional
    public void roll() {
        String username = gameUtilService.getUsername();
        Game game = tablaUtilService.findActiveGame(username);
        TablaGameState state = game.getTablaState();
        Player player = requireInTurn(game, username);

        if (state.isRolled()) {
            throw TablaException.diceAlreadyRolled();
        }

        Dice dice = diceService.roll(game.getServerSeed(), game.getId(), state.getTurnIndex());
        state.setTurnIndex(state.getTurnIndex() + 1);
        state.setDie1(dice.d1());
        state.setDie2(dice.d2());
        state.setRemainingDiceValues(dice.values());
        state.setPendingHopList(List.of());
        state.snapshotTurnStart();

        Side side = tablaUtilService.sideOf(game, player);
        state.setMaxDiceUsable(BackgammonRules.maxUsed(state.boardState(), side, dice.values()));

        // The clock is extended on roll and confirm only — see move()/undo().
        state.extendNextMoveTime();
        gameInactivityService.updateNextMoveTime(game);

        if (state.getMaxDiceUsable() == 0) {
            // Completely blocked: nothing to play, so the turn passes immediately.
            tablaUtilService.pushToBoth(game);
            endTurn(game);
            return;
        }

        tablaUtilService.pushToBoth(game);
    }

    @Transactional
    public void move(MoveRequest request) {
        String username = gameUtilService.getUsername();
        Game game = tablaUtilService.findActiveGame(username);
        TablaGameState state = game.getTablaState();
        Player player = requireInTurn(game, username);

        if (!state.isRolled()) {
            throw TablaException.diceNotRolled();
        }

        Side side = tablaUtilService.sideOf(game, player);
        BoardState board = state.boardState();
        int[] remaining = state.remainingDiceValues();
        int used = state.usedDiceCount();

        // The destination is derived here rather than taken from the request, so
        // a client cannot desync the board by sending a mismatched 'to'.
        Hop hop = BackgammonRules
                .legalTurnHops(board, side, remaining, used, state.getMaxDiceUsable())
                .stream()
                .filter(h -> h.from() == request.getFrom() && h.die() == request.getDie())
                .findFirst()
                .orElseThrow(TablaException::illegalHop);

        BoardState after = BackgammonRules.apply(board, side, hop);
        state.setBoardState(after);
        state.setRemainingDiceValues(Dice.without(remaining, hop.die()));

        List<Hop> pending = state.pendingHopList();
        pending.add(hop);
        state.setPendingHopList(pending);

        // Deliberately no clock extension: a player could otherwise stall forever
        // by moving and undoing. One 33s budget covers the whole turn.
        tablaUtilService.pushToBoth(game);

        if (state.usedDiceCount() >= state.getMaxDiceUsable()) {
            confirmInternal(game, state, side);
        }
    }

    @Transactional
    public void undo() {
        String username = gameUtilService.getUsername();
        Game game = tablaUtilService.findActiveGame(username);
        TablaGameState state = game.getTablaState();
        Player player = requireInTurn(game, username);

        List<Hop> pending = state.pendingHopList();
        if (pending.isEmpty()) {
            throw TablaException.nothingToUndo();
        }

        // Replay from the turn-start snapshot rather than inverse-applying the
        // last hop: undoing a hit would otherwise have to remember whether the
        // destination held exactly one enemy checker.
        pending.removeLast();
        Side side = tablaUtilService.sideOf(game, player);
        BoardState board = state.turnStartBoardState();
        int[] remaining = new Dice(state.getDie1(), state.getDie2()).values();

        for (Hop hop : pending) {
            board = BackgammonRules.apply(board, side, hop);
            remaining = Dice.without(remaining, hop.die());
        }

        state.setBoardState(board);
        state.setRemainingDiceValues(remaining);
        state.setPendingHopList(pending);

        tablaUtilService.pushToBoth(game);
    }

    @Transactional
    public void confirm() {
        String username = gameUtilService.getUsername();
        Game game = tablaUtilService.findActiveGame(username);
        TablaGameState state = game.getTablaState();
        Player player = requireInTurn(game, username);

        confirmInternal(game, state, tablaUtilService.sideOf(game, player));
    }

    private void confirmInternal(Game game, TablaGameState state, Side side) {
        if (state.usedDiceCount() != state.getMaxDiceUsable()) {
            throw TablaException.turnNotComplete(state.usedDiceCount(), state.getMaxDiceUsable());
        }

        if (BackgammonRules.isFinished(state.boardState(), side)) {
            Player winner = state.getInTurnPlayer();
            state.clearTurn();
            tablaUtilService.finishGame(game, winner, false);
            gameInactivityService.cancel(game.getId());
            return;
        }

        endTurn(game);
    }

    private void endTurn(Game game) {
        TablaGameState state = game.getTablaState();
        Player next = game.getOpponent(state.getInTurnPlayer());

        state.clearTurn();
        state.setInTurnPlayer(next);

        gameInactivityService.updateNextMoveTime(game);
        tablaUtilService.pushToBoth(game);
    }

    /* ------------------------------------------------------------------
       Leaving and timing out
       ------------------------------------------------------------------ */

    @Transactional
    public void surrender() {
        String username = gameUtilService.getUsername();
        Game game = tablaUtilService.findActiveGame(username);

        if (game.getWinner() != null) {
            return;
        }

        Player opponent = game.getOpponentPlayerByUsername(username);
        tablaUtilService.finishGame(game, opponent, true);
        gameInactivityService.cancel(game.getId());
    }

    @Transactional
    public void reportInactivity() {
        String username = gameUtilService.getUsername();
        Game game = tablaUtilService.findActiveGame(username);
        Player player = game.getPlayerByUsername(username);

        int count = (player.getInactivityCount() == null ? 0 : player.getInactivityCount()) + 1;
        player.setInactivityCount(count);

        if (count >= MAX_INACTIVITY) {
            throw new PlayerInactivitySurrenderException();
        }

        tablaUtilService.pushToBoth(game);
    }

    @Transactional
    public void extendTime() {
        String username = gameUtilService.getUsername();
        Game game = tablaUtilService.findActiveGame(username);
        requireInTurn(game, username);

        game.getTablaState().extendNextMoveTime();
        gameInactivityService.updateNextMoveTime(game);
        tablaUtilService.pushToBoth(game);
    }

    private Player requireInTurn(Game game, String username) {
        Player player = game.getPlayerByUsername(username);
        if (!game.getTablaState().isInTurn(player)) {
            throw TablaException.notYourTurn();
        }
        return player;
    }
}
