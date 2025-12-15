package com.bussiness.santaseservice.service;

import com.bussiness.santaseservice.enums.Rank;
import com.bussiness.santaseservice.model.Card;
import com.bussiness.santaseservice.model.Game;
import com.bussiness.santaseservice.model.GameState;
import com.bussiness.santaseservice.model.Player;
import com.bussiness.santaseservice.model.request.CloseDeckRequest;
import com.bussiness.santaseservice.model.request.FinishDealRequest;
import com.bussiness.santaseservice.model.request.PlayCardRequest;
import com.bussiness.santaseservice.model.request.ReplaceCardRequest;
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

    private final Queue<String> matchQueue = new LinkedList<>();
    private final Lock queueLock = new ReentrantLock();

    public void getGameState(UUID gameId, String username) {
        Game game = gameUtilService.findGameById(gameId);

        if (!game.getFirstPlayer().getUsername().equals(username) &&
                !game.getSecondPlayer().getUsername().equals(username)) {
            throw new RuntimeException("User is not part of this game");
        }

        gameWebSocketService.updateGameState(game, username);
    }

    public void searchGame(String username) {
        gameUtilService.checkIfUserExists(username); //TODO: And do not participate in unfinished game

        queueLock.lock();
        try {
            if (matchQueue.contains(username)) {
                gameWebSocketService.notifyGameSearch(username, SearchGameResponse.waiting());
            }

            String waitingPlayerUsername = matchQueue.poll();

            if (waitingPlayerUsername == null) {
                matchQueue.offer(username);
                gameWebSocketService.notifyGameSearch(username, SearchGameResponse.waiting());
            } else {
                Player firstPlayer = gameUtilService.findPlayerByUsername(username);
                Player secondPlayer = gameUtilService.findPlayerByUsername(waitingPlayerUsername);

                Game newGame = gameUtilService.startGame(firstPlayer, secondPlayer);
                gameWebSocketService.notifyGameSearch(firstPlayer.getUsername(),
                        SearchGameResponse.started(newGame.getId()));
                gameWebSocketService.notifyGameSearch(secondPlayer.getUsername(),
                        SearchGameResponse.started(newGame.getId()));
            }

        } finally {
            queueLock.unlock();
        }
    }

    @Transactional
    public void playCard(PlayCardRequest playCardRequest) {
        Game game = gameUtilService.findGameById(playCardRequest.getGameId());

        Player player = game.getPlayerByUsername(playCardRequest.getUsername());

        GameState state = game.getState();

        if (!state.getInTurnPlayer().equals(player)) {
            throw new RuntimeException("Not your turn");
        }

        Card cardForRemoval = player.getHand().stream()
                .filter(c -> c.getId().equals(playCardRequest.getCardId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Card not found in player's hand"));
        gameUtilService.removeCardFromHand(game, player, cardForRemoval);
        player.setPlayedCard(cardForRemoval);
        gameUtilService.checkTwentyForty(game, player, cardForRemoval);

        if (game.getFirstPlayer().getPlayedCard() != null && game.getSecondPlayer().getPlayedCard() != null) {
            gameWebSocketService.updateGameState(game, game.getFirstPlayer().getUsername());
            gameWebSocketService.updateGameState(game, game.getSecondPlayer().getUsername());
            try {
                Thread.sleep(Duration.ofSeconds(2));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            gameUtilService.evaluateTrick(game);
        } else {
            state.setInTurnPlayer(game.getOpponent(player));
        }

        gameUtilService.saveGame(game);

        gameWebSocketService.updateGameState(game, game.getFirstPlayer().getUsername());
        gameWebSocketService.updateGameState(game, game.getSecondPlayer().getUsername());
    }

    @Transactional
    public void closeDeck(CloseDeckRequest closeDeckRequest) {
        Game game = gameUtilService.findGameForClosingOrRemoval(closeDeckRequest.getGameId(),
                closeDeckRequest.getUsername());
        Player player = game.getPlayerByUsername(closeDeckRequest.getUsername());

        GameState state = game.getState();
        state.getDeck().clear();
        state.setClosedByPlayer(player);
        gameUtilService.saveGameState(state);

        gameWebSocketService.updateGameState(game, game.getFirstPlayer().getUsername());

        gameWebSocketService.updateGameState(game, game.getSecondPlayer().getUsername());
    }

    @Transactional
    public void replaceCard(ReplaceCardRequest replaceCardRequest) {
        Game game = gameUtilService.findGameForClosingOrRemoval(replaceCardRequest.getGameId(),
                replaceCardRequest.getUsername());
        GameState state = game.getState();

        Player player = game.getPlayerByUsername(replaceCardRequest.getUsername());

        List<Card> playerCards = player.getHand();

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

        gameWebSocketService.updateGameState(game, game.getFirstPlayer().getUsername());

        gameWebSocketService.updateGameState(game, game.getSecondPlayer().getUsername());
    }

    @Transactional
    public void finishDeal(FinishDealRequest finishDealRequest) {
        Game game = gameUtilService.findGameById(finishDealRequest.getGameId());

        GameState state = game.getState();

        Player player = game.getPlayerByUsername(finishDealRequest.getUsername());

        if (!state.getFirstTurnPlayer().equals(player))
            throw new RuntimeException("User is not first in turn in this game");

        if (!state.getInTurnPlayer().equals(player))
            throw new RuntimeException("Not your turn");

        Player opponentPlayer = game.getOpponent(player);

        int pointsAwarded;
        String trickWinner;

        if (player.getScore() >= 66) {
            pointsAwarded = opponentPlayer.getIsBlanked() ? 3 : (opponentPlayer.getScore() < 33 ? 2 : 1);
            player.setResult(player.getResult() + pointsAwarded);
            trickWinner = player.getUsername();
        } else {
            opponentPlayer.setResult(opponentPlayer.getResult() + 3);
            trickWinner = opponentPlayer.getUsername();
        }

        gameWebSocketService.updateGameState(game, game.getFirstPlayer().getUsername(),
                trickWinner, game.getFirstPlayer().getScore(), game.getSecondPlayer().getScore());

        gameWebSocketService.updateGameState(game, game.getSecondPlayer().getUsername(),
                trickWinner, game.getFirstPlayer().getScore(), game.getSecondPlayer().getScore());

        gameUtilService.prepareNewState(game, game.getFirstPlayer());
    }
}
