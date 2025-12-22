package com.bussiness.santaseservice.service;

import com.bussiness.santaseservice.enums.Rank;
import com.bussiness.santaseservice.model.Card;
import com.bussiness.santaseservice.model.Game;
import com.bussiness.santaseservice.model.GameState;
import com.bussiness.santaseservice.model.Player;
import com.bussiness.santaseservice.model.request.PlayCardRequest;
import com.bussiness.santaseservice.model.response.SearchGameResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Log4j2
@RequiredArgsConstructor
@Service
public class GameService {
    private final GameWebSocketService gameWebSocketService;
    private final GameUtilService gameUtilService;

    private final Queue<String> matchQueue = new LinkedList<>();
    private final Lock queueLock = new ReentrantLock();

    public void getGameState() {
        String username = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
        log.info("Trying to get game state id: {}", username);

        Game game = gameUtilService.findGameByUsername(username);

        if (!game.getFirstPlayer().getUsername().equals(username) &&
                !game.getSecondPlayer().getUsername().equals(username)) {
            throw new RuntimeException("User is not part of this game");
        }

        gameWebSocketService.updateGameState(game, username);
    }

    public void searchGame() {
        String username = gameUtilService.getUsername();
        log.info("Trying to start game, account with username {}", username);

        gameUtilService.checkIfUserExistsAndIsAvailable(username);

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
        String username = gameUtilService.getUsername();

        Game game = gameUtilService.findGameByUsername(username);

        Player player = game.getPlayerByUsername(username);

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
    public void closeDeck() {
        String username = gameUtilService.getUsername();
        log.info("Trying to close deck: {}", username);

        Game game = gameUtilService.findGameForClosingOrRemoval(username);
        Player player = game.getPlayerByUsername(username);

        GameState state = game.getState();
        state.getDeck().clear();
        state.setClosedByPlayer(player);
        gameUtilService.saveGameState(state);

        gameWebSocketService.updateGameState(game, game.getFirstPlayer().getUsername());

        gameWebSocketService.updateGameState(game, game.getSecondPlayer().getUsername());
    }

    @Transactional
    public void replaceCard() {
        String username = gameUtilService.getUsername();
        log.info("Trying to replace card: {}", username);

        Game game = gameUtilService.findGameForClosingOrRemoval(username);
        GameState state = game.getState();

        Player player = game.getPlayerByUsername(username);

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
    public void finishDeal() {
        String username = gameUtilService.getUsername();
        log.info("Trying to finish deal: {}", username);

        Game game = gameUtilService.findGameByUsername(username);

        GameState state = game.getState();

        Player player = game.getPlayerByUsername(username);

        if (!state.getFirstTurnPlayer().equals(player))
            throw new RuntimeException("User is not first in turn in this game");

        if (!state.getInTurnPlayer().equals(player))
            throw new RuntimeException("Not your turn");

        Player opponentPlayer = game.getOpponent(player);

        int pointsAwarded;
        Player trickWinner;

        if (player.getScore() >= 66) {
            pointsAwarded = opponentPlayer.getIsBlanked() ? 3 : (opponentPlayer.getScore() < 33 ? 2 : 1);
            player.setResult(player.getResult() + pointsAwarded);
            trickWinner = player;
        } else {
            opponentPlayer.setResult(opponentPlayer.getResult() + 3);
            trickWinner = opponentPlayer;
        }

        gameWebSocketService.updateGameState(game, game.getFirstPlayer().getUsername(),
                trickWinner.getUsername(), game.getFirstPlayer().getScore(), game.getSecondPlayer().getScore());

        gameWebSocketService.updateGameState(game, game.getSecondPlayer().getUsername(),
                trickWinner.getUsername(), game.getFirstPlayer().getScore(), game.getSecondPlayer().getScore());

        gameUtilService.prepareNewState(game, trickWinner);
        gameUtilService.saveGame(game);

        gameWebSocketService.updateGameState(game, game.getFirstPlayer().getUsername());
        gameWebSocketService.updateGameState(game, game.getSecondPlayer().getUsername());
    }

    @Transactional
    public void finishGame() {
        String username = gameUtilService.getUsername();
        log.info("Trying to finish game: {}", username);
        Game game = gameUtilService.findGameByUsername(username);

        Player opponentPlayer = game.getOpponentPlayerByUsername(username);
        game.setWinner(opponentPlayer);
        gameUtilService.saveGame(game);

        gameWebSocketService.updateGameState(game, game.getFirstPlayer().getUsername());

        gameWebSocketService.updateGameState(game, game.getSecondPlayer().getUsername());
    }
}
