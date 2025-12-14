package com.bussiness.santaseservice.service;

import com.bussiness.santaseservice.enums.Rank;
import com.bussiness.santaseservice.model.Card;
import com.bussiness.santaseservice.model.Game;
import com.bussiness.santaseservice.model.GameState;
import com.bussiness.santaseservice.model.User;
import com.bussiness.santaseservice.model.request.CloseDeckRequest;
import com.bussiness.santaseservice.model.request.FinishDealRequest;
import com.bussiness.santaseservice.model.request.PlayCardRequest;
import com.bussiness.santaseservice.model.request.ReplaceCardRequest;
import com.bussiness.santaseservice.model.response.GameStateResponse;
import com.bussiness.santaseservice.model.response.SearchGameResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@RequiredArgsConstructor
@Service
public class GameService {
    private final GameWebSocketService gameWebSocketService;
    private final GameUtilService gameUtilService;

    private final Queue<User> matchQueue = new LinkedList<>();
    private final Lock queueLock = new ReentrantLock();

    public GameStateResponse getGameState(UUID gameId, String username) {
        Game game = gameUtilService.findGameById(gameId);

        if (!game.getFirstPlayer().getUsername().equals(username) &&
                !game.getSecondPlayer().getUsername().equals(username)) {
            throw new RuntimeException("User is not part of this game");
        }

        return gameWebSocketService.updateGameState(game, username);
    }

    public SearchGameResponse searchGame(String username) {
        User user = gameUtilService.findUserByUsername(username);

        queueLock.lock();
        try {
            if (matchQueue.contains(user)) {
                gameWebSocketService.notifyGameSearch(username, SearchGameResponse.waiting());
                return SearchGameResponse.waiting();
            }

            User waitingUser = matchQueue.poll();

            if (waitingUser == null) {
                matchQueue.offer(user);
                gameWebSocketService.notifyGameSearch(username, SearchGameResponse.waiting());
                return SearchGameResponse.waiting();
            } else {
                Game newGame = gameUtilService.startGame(user, waitingUser);
                gameWebSocketService.notifyGameSearch(newGame.getFirstPlayer().getUsername(),
                        SearchGameResponse.started(newGame.getId()));
                gameWebSocketService.notifyGameSearch(newGame.getSecondPlayer().getUsername(),
                        SearchGameResponse.started(newGame.getId()));
                return SearchGameResponse.started(newGame.getId());
            }

        } finally {
            queueLock.unlock();
        }
    }

    @Transactional
    public GameStateResponse playCard(PlayCardRequest playCardRequest) {
        Game game = gameUtilService.findGameById(playCardRequest.getGameId());

        GameState state = game.getState();

        // Check turn
        if (!state.getInTurnPlayerUsername().equals(playCardRequest.getUsername())) {
            throw new RuntimeException("Not your turn");
        }

        boolean isFirstPlayer = game.getFirstPlayer().getUsername()
                .equals(playCardRequest.getUsername());
        boolean isSecondPlayer = game.getSecondPlayer().getUsername()
                .equals(playCardRequest.getUsername());

        if (!isFirstPlayer && !isSecondPlayer) {
            throw new RuntimeException("User not in this game");
        }

        // Remove card from player's hand
        if (isFirstPlayer) {
            Card card = state.getFirstPlayerHand().stream()
                    .filter(c -> c.getId().equals(playCardRequest.getCardId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Card not found in player's hand"));
            gameUtilService.removeCardFromHand(state.getFirstPlayerHand(),
                    game.getFirstPlayer().getUsername(), card, game);
            state.setFirstPlayerPlayedCard(card);
            gameUtilService.checkTwentyForty(game, game.getFirstPlayer().getUsername(), card);
        } else {
            Card card = state.getSecondPlayerHand().stream()
                    .filter(c -> c.getId().equals(playCardRequest.getCardId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Card not found in player's hand"));
            gameUtilService.removeCardFromHand(state.getSecondPlayerHand(),
                    game.getSecondPlayer().getUsername(), card, game);
            state.setSecondPlayerPlayedCard(card);
            gameUtilService.checkTwentyForty(game, game.getSecondPlayer().getUsername(), card);
        }

        // If both players have played → evaluate trick
        if (state.getFirstPlayerPlayedCard() != null && state.getSecondPlayerPlayedCard() != null) {
            gameWebSocketService.updateGameState(game, game.getFirstPlayer().getUsername());
            gameWebSocketService.updateGameState(game, game.getSecondPlayer().getUsername());
            try {
                Thread.sleep(Duration.ofSeconds(2));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            gameUtilService.evaluateTrick(game);
        } else {
            if (isFirstPlayer) {
                state.setInTurnPlayerUsername(game.getSecondPlayer().getUsername());
            } else {
                state.setInTurnPlayerUsername(game.getFirstPlayer().getUsername());
            }
        }

        gameUtilService.saveGame(game);

        GameStateResponse firstPlayerResponse = gameWebSocketService
                .updateGameState(game, game.getFirstPlayer().getUsername());

        GameStateResponse secondPlayerResponse = gameWebSocketService
                .updateGameState(game, game.getSecondPlayer().getUsername());

        return isFirstPlayer ? firstPlayerResponse : secondPlayerResponse;
    }

    @Transactional
    public GameStateResponse closeDeck(CloseDeckRequest closeDeckRequest) {
        Game game = gameUtilService.findGameForClosingOrRemoval(closeDeckRequest.getGameId(),
                closeDeckRequest.getUsername());

        GameState state = game.getState();
        state.getDeck().clear();
        state.setClosedByUsername(closeDeckRequest.getUsername());
        gameUtilService.saveGameState(state);

        boolean isFirstPlayer = game.getFirstPlayer().getUsername()
                .equals(closeDeckRequest.getUsername());

        GameStateResponse firstPlayerResponse = gameWebSocketService
                .updateGameState(game, game.getFirstPlayer().getUsername());

        GameStateResponse secondPlayerResponse = gameWebSocketService
                .updateGameState(game, game.getSecondPlayer().getUsername());

        return isFirstPlayer ? firstPlayerResponse : secondPlayerResponse;
    }

    @Transactional
    public GameStateResponse replaceCard(ReplaceCardRequest replaceCardRequest) {
        Game game = gameUtilService.findGameForClosingOrRemoval(replaceCardRequest.getGameId(),
                replaceCardRequest.getUsername());
        GameState state = game.getState();

        boolean isFirstPlayer = game.getFirstPlayer().getUsername()
                .equals(replaceCardRequest.getUsername());
        List<Card> playerCards = isFirstPlayer ? state.getFirstPlayerHand() : state.getSecondPlayerHand();

        Card cardForReplace = playerCards.stream()
                .filter(card -> card.getRank() == Rank.NINE)
                .filter(card -> card.getSuit() == state.getTrumpCard().getSuit())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("There is no card for replace"));

        playerCards.remove(cardForReplace);
        playerCards.add(state.getTrumpCard());

        state.setTrumpCard(cardForReplace);

        state.getDeck().removeLast();
        state.getDeck().addLast(cardForReplace);
        gameUtilService.saveGameState(state);

        GameStateResponse firstPlayerResponse = gameWebSocketService
                .updateGameState(game, game.getFirstPlayer().getUsername());

        GameStateResponse secondPlayerResponse = gameWebSocketService
                .updateGameState(game, game.getSecondPlayer().getUsername());

        return isFirstPlayer ? firstPlayerResponse : secondPlayerResponse;
    }

    @Transactional
    public GameStateResponse finishDeal(FinishDealRequest finishDealRequest) {
        Game game = gameUtilService.findGameById(finishDealRequest.getGameId());

        GameState state = game.getState();

        String user = finishDealRequest.getUsername();

        if (!state.getFirstTurnPlayerUsername().equals(user))
            throw new RuntimeException("User is not first in turn in this game");

        if (!state.getInTurnPlayerUsername().equals(user))
            throw new RuntimeException("Not your turn");

        boolean isFirstPlayer = game.getFirstPlayer().getUsername().equals(user);
        int firstPlayerScore = state.getFirstPlayerScore();
        int secondPlayerScore = state.getSecondPlayerScore();

        int playerScore = isFirstPlayer ? firstPlayerScore : secondPlayerScore;
        int opponentScore = isFirstPlayer ? secondPlayerScore : firstPlayerScore;
        boolean opponentBlanked = isFirstPlayer ? state.getIsSecondPlayerBlanked()
                : state.getIsFirstPlayerBlanked();

        // Determine round points
        int pointsAwarded;

        if (playerScore >= 66) {
            // Player wins normally
            pointsAwarded = opponentBlanked ? 3 : (opponentScore < 33 ? 2 : 1);
        } else {
            // Opponent wins (player failed to reach 66)
            pointsAwarded = 3;
            isFirstPlayer = !isFirstPlayer;  // flip winner
        }

        // Apply result
        if (isFirstPlayer) {
            if (state.isClosed() && state.getClosedByUsername().equals(game.getSecondPlayer().getUsername())) {
                pointsAwarded = 3;
            }
            game.setFirstPlayerResult(game.getFirstPlayerResult() + pointsAwarded);
            gameUtilService.prepareNewState(game, game.getFirstPlayer());
        } else {
            if (state.isClosed() && state.getClosedByUsername().equals(game.getFirstPlayer().getUsername())) {
                pointsAwarded = 3;
            }
            game.setSecondPlayerResult(game.getSecondPlayerResult() + pointsAwarded);
            gameUtilService.prepareNewState(game, game.getSecondPlayer());
        }

        String trickWinner = isFirstPlayer ?
                game.getFirstPlayer().getUsername() : game.getSecondPlayer().getUsername();

        GameStateResponse firstPlayerResponse = gameWebSocketService
                .updateGameState(game, game.getFirstPlayer().getUsername(),
                        trickWinner, firstPlayerScore, secondPlayerScore);

        GameStateResponse secondPlayerResponse = gameWebSocketService
                .updateGameState(game, game.getSecondPlayer().getUsername(),
                        trickWinner, firstPlayerScore, secondPlayerScore);

        return isFirstPlayer ? firstPlayerResponse : secondPlayerResponse;
    }
}
