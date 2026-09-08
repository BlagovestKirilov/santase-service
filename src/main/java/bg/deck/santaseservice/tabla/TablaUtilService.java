package bg.deck.santaseservice.tabla;

import bg.deck.santaseservice.enums.GameType;
import bg.deck.santaseservice.exception.NoActiveGameFoundException;
import bg.deck.santaseservice.model.Game;
import bg.deck.santaseservice.model.Player;
import bg.deck.santaseservice.model.TablaGameState;
import bg.deck.santaseservice.model.response.HopDTO;
import bg.deck.santaseservice.model.response.TablaStateResponse;
import bg.deck.santaseservice.repository.GameRepository;
import bg.deck.santaseservice.service.RankingService;
import bg.deck.santaseservice.service.WebSocketService;
import bg.deck.santaseservice.tabla.engine.BackgammonRules;
import bg.deck.santaseservice.tabla.engine.BoardState;
import bg.deck.santaseservice.tabla.engine.Dice;
import bg.deck.santaseservice.tabla.engine.GameResultKind;
import bg.deck.santaseservice.tabla.engine.Hop;
import bg.deck.santaseservice.tabla.engine.Side;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Table lifecycle for табла: creating games, building the per-player payload and
 * ending games. {@link TablaService} holds the request-facing operations.
 */
@Log4j2
@RequiredArgsConstructor
@Service
public class TablaUtilService {

    private final GameRepository gameRepository;
    private final WebSocketService webSocketService;
    private final RankingService rankingService;
    private final TablaDiceService diceService;

    /* ------------------------------------------------------------------
       Lifecycle
       ------------------------------------------------------------------ */

    /**
     * Starts a game with the opening roll already applied: each side rolls one
     * die, the higher moves first and plays both. That means the very first push
     * already carries dice, which is also better UX than an extra "roll" step.
     */
    @Transactional
    public Game startGame(Player firstPlayer, Player secondPlayer) {
        firstPlayer.setInactivityCount(0);
        secondPlayer.setInactivityCount(0);

        byte[] seed = diceService.newSeed();

        TablaGameState state = TablaGameState.builder()
                .board(BoardState.initial().encode())
                .turnIndex(0)
                .maxDiceUsable(0)
                .build();

        Game game = Game.builder()
                .gameType(GameType.TABLA)
                .firstPlayer(firstPlayer)
                .secondPlayer(secondPlayer)
                .tablaState(state)
                .serverSeed(seed)
                .serverSeedHash(diceService.hash(seed))
                .build();

        game = gameRepository.save(game);

        applyOpeningRoll(game);
        return gameRepository.save(game);
    }

    private void applyOpeningRoll(Game game) {
        TablaGameState state = game.getTablaState();
        int index = diceService.openingRollIndexUsed(game.getServerSeed(), game.getId(), 0);
        Dice dice = diceService.roll(game.getServerSeed(), game.getId(), index);

        // The higher single die decides who starts; first player is WHITE.
        Player starter = dice.d1() > dice.d2() ? game.getFirstPlayer() : game.getSecondPlayer();

        state.setFirstTurnPlayer(starter);
        state.setDie1(dice.d1());
        state.setDie2(dice.d2());
        state.setRemainingDiceValues(dice.values());
        state.setTurnIndex(index + 1);
        state.snapshotTurnStart();
        state.setInTurnPlayer(starter);

        BoardState board = state.boardState();
        state.setMaxDiceUsable(BackgammonRules.maxUsed(board, sideOf(game, starter), dice.values()));
    }

    public Game findActiveGame(String username) {
        return gameRepository.findActiveGamesByUsernameAndType(username, GameType.TABLA)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NoActiveGameFoundException(username));
    }

    public Side sideOf(Game game, Player player) {
        return game.getFirstPlayer().equals(player) ? Side.WHITE : Side.BLACK;
    }

    /* ------------------------------------------------------------------
       Ending
       ------------------------------------------------------------------ */

    @Transactional
    public void finishGame(Game game, Player winner, boolean opponentSurrendered) {
        if (game.getWinner() != null) {
            return;
        }
        game.setWinner(winner, opponentSurrendered);
        rankingService.updateRankingAfterGame(game);
        gameRepository.save(game);
        pushToBoth(game);
    }

    /**
     * Called by the scheduler when a player's clock runs out. Mirrors the Santase
     * path, including the guard against a game that already finished normally.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void surrenderByInactivity(UUID gameId) {
        Game game = gameRepository.findById(gameId).orElse(null);
        if (game == null || game.getWinner() != null) {
            return;
        }

        Player timedOut = game.getTablaState().getInTurnPlayer();
        Player opponent = game.getOpponent(timedOut);

        log.info("Табла: {} timed out, {} wins", timedOut.getUsername(), opponent.getUsername());
        finishGame(game, opponent, true);
    }

    /* ------------------------------------------------------------------
       Pushing state
       ------------------------------------------------------------------ */

    public void pushToBoth(Game game) {
        push(game, game.getFirstPlayer().getUsername());
        push(game, game.getSecondPlayer().getUsername());
    }

    public void push(Game game, String username) {
        webSocketService.notifyGameUpdate(game.getId().toString(), username, buildState(game, username));
    }

    /** The position from one player's point of view, with their legal moves. */
    public TablaStateResponse buildState(Game game, String username) {
        Player player = game.getPlayerByUsername(username);
        Player opponent = game.getOpponent(player);
        TablaGameState state = game.getTablaState();

        Side side = sideOf(game, player);
        Side other = side.opponent();
        BoardState board = state.boardState();

        boolean onTurn = state.isInTurn(player);
        int[] remaining = state.remainingDiceValues();
        int used = state.usedDiceCount();

        List<HopDTO> legal = onTurn && game.getWinner() == null
                ? BackgammonRules.legalTurnHops(board, side, remaining, used, state.getMaxDiceUsable())
                        .stream().map(HopDTO::from).toList()
                : List.of();

        List<HopDTO> pending = state.pendingHopList().stream().map(HopDTO::from).toList();

        GameResultKind kind = game.getWinner() == null
                ? null
                : BackgammonRules.resultKind(board, sideOf(game, game.getWinner()));

        return TablaStateResponse.builder()
                .gameId(game.getId().toString())
                .gameType(GameType.TABLA.name())
                .firstPlayerUsername(game.getFirstPlayer().getUsername())
                .secondPlayerUsername(game.getSecondPlayer().getUsername())
                .mySide(side.name())
                .points(IntStream.rangeClosed(1, BoardState.POINTS).boxed().map(board::at).toList())
                .myBar(board.bar(side))
                .opponentBar(board.bar(other))
                .myOff(board.off(side))
                .opponentOff(board.off(other))
                .myPipCount(board.pipCount(side))
                .opponentPipCount(board.pipCount(other))
                .isOnTurn(onTurn)
                .die1(state.getDie1())
                .die2(state.getDie2())
                .remainingDice(Arrays.stream(remaining).boxed().toList())
                .maxDiceUsable(state.getMaxDiceUsable())
                .usedDiceCount(used)
                .mustConfirm(onTurn && state.isRolled() && used == state.getMaxDiceUsable()
                        && state.getMaxDiceUsable() > 0)
                .noMovesAvailable(onTurn && state.isRolled() && state.getMaxDiceUsable() == 0)
                .legalHops(legal)
                .pendingHops(pending)
                .winnerUsername(game.getWinner() != null ? game.getWinner().getUsername() : null)
                .surrenderPlayerUsername(game.getSurrenderPlayer() != null
                        ? game.getSurrenderPlayer().getUsername() : null)
                .resultKind(kind != null ? kind.name() : null)
                .inactivityCount(player.getInactivityCount() != null ? player.getInactivityCount() : 0)
                .nextMoveTimeInSeconds(onTurn && state.getNextMoveTime() != null
                        ? Math.toIntExact(Math.max(0,
                                Duration.between(Instant.now(), state.getNextMoveTime()).getSeconds()))
                        : null)
                .serverSeedHash(game.getServerSeedHash())
                // The seed is what makes past rolls verifiable, so it must stay
                // secret until there are no future rolls left to predict.
                .serverSeed(game.getWinner() != null && game.getServerSeed() != null
                        ? HexFormat.of().formatHex(game.getServerSeed()) : null)
                .build();
    }

    /** Convenience for tests and the service layer. */
    public List<Hop> legalHops(Game game, Player player) {
        TablaGameState state = game.getTablaState();
        return BackgammonRules.legalTurnHops(state.boardState(), sideOf(game, player),
                state.remainingDiceValues(), state.usedDiceCount(), state.getMaxDiceUsable());
    }
}
