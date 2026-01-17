package bg.deck.santaseservice.service;

import bg.deck.santaseservice.constant.LogConstants;
import bg.deck.santaseservice.enums.card.Rank;
import bg.deck.santaseservice.exception.CardNotFoundException;
import bg.deck.santaseservice.exception.NoCardForReplacingException;
import bg.deck.santaseservice.exception.NotFirstInTurnException;
import bg.deck.santaseservice.exception.NotInTurnException;
import bg.deck.santaseservice.exception.UserNotPartOfGameException;
import bg.deck.santaseservice.model.Card;
import bg.deck.santaseservice.model.Game;
import bg.deck.santaseservice.model.GameState;
import bg.deck.santaseservice.model.Player;
import bg.deck.santaseservice.model.request.CardRequest;
import bg.deck.santaseservice.model.response.SearchGameResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Log4j2
@RequiredArgsConstructor
@Service
public class GameService {
    private final WebSocketService webSocketService;
    private final WebSocketUtilService webSocketUtilService;
    private final GameUtilService gameUtilService;

    private final Queue<String> matchQueue = new ConcurrentLinkedQueue<>();

    public void getGameState() {
        String username = gameUtilService.getUsername();

        log.info(LogConstants.GET_STATE_LOG, username);

        Game game = gameUtilService.findGameByUsername(username);

        if (!game.getFirstPlayer().getUsername().equals(username) && !game.getSecondPlayer().getUsername().equals(username)) {
            throw new UserNotPartOfGameException(username);
        }

        webSocketUtilService.updateGameState(game, username);

        log.info(LogConstants.GOT_STATE_LOG, username);
    }

    public void searchGame() {
        String username = gameUtilService.getUsername();
        log.info(LogConstants.GAME_SEARCH_START, username);

        if (!gameUtilService.checkIfUserExistsAndIsAvailable(username)) {
            log.warn(LogConstants.GAME_SEARCH_USER_UNAVAILABLE, username);
            return;
        }

        if (matchQueue.contains(username)) {
            log.info(LogConstants.GAME_SEARCH_ALREADY_IN_QUEUE, username);
            webSocketService.notifyGameSearch(username, SearchGameResponse.waiting());
            return;
        }

        String waitingPlayerUsername = matchQueue.poll();

        if (waitingPlayerUsername == null) {
            matchQueue.offer(username);
            log.info(LogConstants.GAME_SEARCH_ADDED_TO_QUEUE, username);
            webSocketService.notifyGameSearch(username, SearchGameResponse.waiting());
        } else {
            Player firstPlayer = gameUtilService.findPlayerByUsername(username);
            Player secondPlayer = gameUtilService.findPlayerByUsername(waitingPlayerUsername);

            Game newGame = gameUtilService.startGame(firstPlayer, secondPlayer);

            log.info(LogConstants.GAME_SEARCH_MATCH_FOUND,
                    firstPlayer.getUsername(),
                    secondPlayer.getUsername(),
                    newGame.getId());

            webSocketService.notifyGameSearch(List.of(firstPlayer.getUsername(), secondPlayer.getUsername()),
                    SearchGameResponse.started(newGame.getId()));
        }
    }

    @Transactional
    public void playCard(CardRequest cardRequest) {
        String username = gameUtilService.getUsername();
        log.info(LogConstants.PLAY_CARD_START, username, cardRequest.getCardId());

        Game game = gameUtilService.findGameByUsername(username);
        Player player = game.getPlayerByUsername(username);
        GameState state = game.getState();

        if (!state.getInTurnPlayer().equals(player)) {
            throw new NotInTurnException(username);
        }

        Card cardForRemoval = player.getHand().stream()
                .filter(c -> c.getId().equals(cardRequest.getCardId()))
                .filter(Card::getIsPlayable)
                .findFirst()
                .orElseThrow(() -> new CardNotFoundException(username));

        gameUtilService.removeCardFromHand(game, player, cardForRemoval);
        player.setPlayedCard(cardForRemoval);
        player.getHand().forEach(card -> card.setIsPlayable(true));

        if (game.getFirstPlayer().getPlayedCard() != null && game.getSecondPlayer().getPlayedCard() != null) {
            log.info(LogConstants.PLAY_CARD_TRICK_EVALUATING,
                    game.getFirstPlayer().getPlayedCard(), game.getSecondPlayer().getPlayedCard());

            webSocketUtilService.updateGameState(game);
            gameUtilService.evaluateTrick(game);
        } else {
            log.info(LogConstants.PLAY_CARD_SUCCESS, username, cardForRemoval.getId());
            state.setInTurnPlayer(game.getOpponent(player));
        }

        gameUtilService.saveGame(game);

        webSocketUtilService.updateGameState(game);
    }

    @Transactional
    public void announceCombination(CardRequest cardRequest) {
        String username = gameUtilService.getUsername();
        log.info(LogConstants.ANNOUNCE_START, username, cardRequest.getCardId());

        Game game = gameUtilService.findGameByUsername(username);

        Player player = game.getPlayerByUsername(username);

        GameState state = game.getState();

        if (!state.getInTurnPlayer().equals(player)) {
            throw new NotInTurnException(username);
        }

        if (!state.getFirstTurnPlayer().equals(player)) {
            throw new NotFirstInTurnException(username);
        }

        Card card = player.getHand().stream()
                .filter(c -> c.getId().equals(cardRequest.getCardId()))
                .findFirst()
                .orElseThrow(() -> new CardNotFoundException(username));

        if (gameUtilService.checkTwentyForty(game, player, card)) {
            log.info(LogConstants.ANNOUNCE_SUCCESS, username, player.getBonus());

            webSocketUtilService.updateGameState(game);

            player.setBonus(null);
            gameUtilService.saveGame(game);
        }
    }

    @Transactional
    public void closeDeck() {
        String username = gameUtilService.getUsername();
        log.info(LogConstants.CLOSE_DECK_START, username);

        Game game = gameUtilService.findGameForClosingOrRemoval(username);
        Player player = game.getPlayerByUsername(username);

        GameState state = game.getState();
        state.getDeck().clear();
        state.setClosedByPlayer(player);
        gameUtilService.saveGameState(state);

        log.info(LogConstants.CLOSE_DECK_SUCCESS, username);

        webSocketUtilService.updateGameState(game);
    }

    @Transactional
    public void replaceCard() {
        String username = gameUtilService.getUsername();

        Game game = gameUtilService.findGameForClosingOrRemoval(username);
        GameState state = game.getState();
        Player player = game.getPlayerByUsername(username);

        log.info(LogConstants.REPLACE_CARD_START, username, state.getTrumpCard().getSuit());

        List<Card> playerCards = player.getHand();

        Card nineOfTrumps = playerCards.stream()
                .filter(card -> card.getRank() == Rank.NINE)
                .filter(card -> card.getSuit() == state.getTrumpCard().getSuit())
                .findFirst()
                .orElseThrow(() -> new NoCardForReplacingException(username));

        Card currentTrump = state.getTrumpCard();

        playerCards.remove(nineOfTrumps);
        playerCards.add(currentTrump);

        state.setTrumpCard(nineOfTrumps);

        state.getDeck().removeLast();
        state.getDeck().addLast(nineOfTrumps);
        gameUtilService.saveGameState(state);
        log.info(LogConstants.REPLACE_CARD_SUCCESS, username, currentTrump.getRank());

        webSocketUtilService.updateGameState(game);
    }

    @Transactional
    public void finishDeal() {
        String username = gameUtilService.getUsername();
        log.info(LogConstants.FINISH_DEAL_START, username);

        Game game = gameUtilService.findGameByUsername(username);
        GameState state = game.getState();
        Player player = game.getPlayerByUsername(username);

        if (!state.getFirstTurnPlayer().equals(player)) {
            throw new NotFirstInTurnException(username);
        }

        if (!state.getInTurnPlayer().equals(player)) {
            throw new NotInTurnException(username);
        }

        Player opponentPlayer = game.getOpponent(player);
        int pointsAwarded;
        Player trickWinner;

        if (player.getScore() >= 66) {
            boolean isBlanked = opponentPlayer.getIsBlanked();
            if (isBlanked) {
                pointsAwarded = 3;
            } else if (opponentPlayer.getScore() < 33) {
                pointsAwarded = 2;
            } else {
                pointsAwarded = 1;
            }
            player.setResult(player.getResult() + pointsAwarded);
            trickWinner = player;
        } else {
            pointsAwarded = 3;
            opponentPlayer.setResult(opponentPlayer.getResult() + pointsAwarded);
            trickWinner = opponentPlayer;
        }

        log.info(LogConstants.FINISH_DEAL_SUCCESS, trickWinner.getUsername(), pointsAwarded);

        webSocketUtilService.updateGameStateWithTrickWinner(game, trickWinner.getUsername());

        gameUtilService.prepareNewState(game, trickWinner);
        gameUtilService.saveGame(game);

        webSocketUtilService.updateGameState(game);
    }

    @Transactional
    public void surrender() {
        String username = gameUtilService.getUsername();
        Game game = gameUtilService.findGameByUsername(username);
        Player opponentPlayer = game.getOpponentPlayerByUsername(username);

        log.info(LogConstants.FINISH_GAME_SURRENDER, username, opponentPlayer.getUsername());

        game.getFirstPlayer().setPlayedCard(null);
        game.getSecondPlayer().setPlayedCard(null);
        game.getFirstPlayer().setHand(new ArrayList<>());
        game.getSecondPlayer().setHand(new ArrayList<>());

        gameUtilService.setGameWinner(game, opponentPlayer, true);
        log.info(
                LogConstants.FINISH_GAME,
                game.getId(),
                opponentPlayer.getUsername(),
                game.getFirstPlayer().getUsername(),
                game.getFirstPlayer().getResult(),
                game.getSecondPlayer().getUsername(),
                game.getSecondPlayer().getResult()
        );

        gameUtilService.saveGame(game);
        webSocketUtilService.updateGameState(game);
    }
}
