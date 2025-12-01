package com.bussiness.santaseservice.service;

import com.bussiness.santaseservice.enums.Rank;
import com.bussiness.santaseservice.enums.Suit;
import com.bussiness.santaseservice.model.Card;
import com.bussiness.santaseservice.model.Game;
import com.bussiness.santaseservice.model.GameState;
import com.bussiness.santaseservice.model.User;
import com.bussiness.santaseservice.model.request.PlayCardRequest;
import com.bussiness.santaseservice.repository.GameRepository;
import com.bussiness.santaseservice.repository.GameStateRepository;
import com.bussiness.santaseservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

@RequiredArgsConstructor
@Service
public class GameService {
    private final GameRepository gameRepo;
    private final UserRepository userRepo;
    private final GameStateRepository gameStateRepo;

    volatile Queue<String> matchQueue = new ConcurrentLinkedQueue<>();

    public GameState getGameState(Long gameId) {
        Game game = gameRepo.findById(gameId).orElseThrow(() -> new RuntimeException("Game id not found"));
        return game.getState();
    }

    @Transactional
    public synchronized Game startGame(String username) {
        String waitingUser = matchQueue.poll();

        if (waitingUser == null) {
            matchQueue.offer(username);
            return null;
        } else {
            return startMatch(waitingUser, username);
        }
    }

    @Transactional
    public Game startMatch(String firstPlayerUsername, String secondPlayerUsername) {
        User firstPlayer = userRepo.findByUsername(firstPlayerUsername).orElseThrow();
        User secondPlayer = userRepo.findByUsername(secondPlayerUsername).orElseThrow();

        List<Card> deck = new ArrayList<>();
        for (Suit s : Suit.values()) {
            for (Rank r : Rank.values()) {
                deck.add(Card.builder().id(UUID.randomUUID()).suit(s).rank(r).build());
            }
        }
        Collections.shuffle(deck);

        GameState gameState = GameState.builder()
                .deck(deck)
                .firstTurnPlayerUsername(firstPlayerUsername)
                .inTurnPlayerUsername(firstPlayerUsername)
                .firstPlayerHand(new ArrayList<>())
                .secondPlayerHand(new ArrayList<>())
                .firstPlayerScore(0)
                .secondPlayerScore(0)
                .build();

        // Deal 6 cards each
        for (int i = 0; i < 6; i++) gameState.getFirstPlayerHand().add(deck.removeFirst());
        for (int i = 0; i < 6; i++) gameState.getSecondPlayerHand().add(deck.removeFirst());

        gameState.setTrumpCard(deck.getFirst());

        Game game = Game.builder()
                .firstPlayer(firstPlayer)
                .secondPlayer(secondPlayer)
                .state(gameState)
                .build();
        gameStateRepo.save(gameState);
        return gameRepo.save(game);
    }

    @Transactional
    public GameState playCard(PlayCardRequest playCardRequest) {
        Game game = gameRepo.findById(playCardRequest.getGameId())
                .orElseThrow(() -> new RuntimeException("Game not found"));

        GameState state = game.getState();

        // Check turn
        if (!state.getInTurnPlayerUsername().equals(playCardRequest.getUsername())) {
            throw new RuntimeException("Not your turn");
        }

        boolean isP1 = game.getFirstPlayer().getUsername().equals(playCardRequest.getUsername());
        boolean isP2 = game.getSecondPlayer().getUsername().equals(playCardRequest.getUsername());

        if (!isP1 && !isP2) {
            throw new RuntimeException("User not in this game");
        }

        // Remove card from player's hand
        if (isP1) {
            Card card = state.getFirstPlayerHand().stream()
                    .filter(c -> c.getId().equals(playCardRequest.getCardId()))
                    .findFirst()
                    .orElse(null);
            if (!state.getFirstPlayerHand().remove(card)) throw new RuntimeException("Card not in player's hand");
            state.setFirstPlayerPlayedCard(card);
            checkTwentyForty(game, game.getFirstPlayer().getUsername(), card);
        } else {
            Card card = state.getSecondPlayerHand().stream()
                    .filter(c -> c.getId().equals(playCardRequest.getCardId()))
                    .findFirst()
                    .orElse(null);
            if (!state.getSecondPlayerHand().remove(card)) throw new RuntimeException("Card not in player's hand");
            state.setSecondPlayerPlayedCard(card);
            checkTwentyForty(game, game.getFirstPlayer().getUsername(), card);
        }

        // If both players have played → evaluate trick
        if (state.getFirstPlayerPlayedCard() != null && state.getSecondPlayerPlayedCard() != null) {
            evaluateTrick(game);
        } else {
            if (isP1) {
                state.setInTurnPlayerUsername(game.getSecondPlayer().getUsername());
            } else {
                state.setInTurnPlayerUsername(game.getFirstPlayer().getUsername());
            }
        }

        game.setState(state);
        gameRepo.save(game);
        return state;
    }

    private void evaluateTrick(Game game) {
        GameState state = game.getState();
        Card firstPlayerCard = state.getFirstPlayerPlayedCard();
        Card secondPlayerCard = state.getSecondPlayerPlayedCard();

        int winner = determineWinner(game);

        if (winner == 1) {
            state.setFirstPlayerScore(state.getFirstPlayerScore() + firstPlayerCard.getPoints() + secondPlayerCard.getPoints());
            state.setInTurnPlayerUsername(game.getFirstPlayer().getUsername());
        } else {
            state.setSecondPlayerScore(state.getSecondPlayerScore() + firstPlayerCard.getPoints() + secondPlayerCard.getPoints());
            state.setInTurnPlayerUsername(game.getSecondPlayer().getUsername());
        }

        // Draw new cards
        if (!state.getDeck().isEmpty()) {
            if (state.getInTurnPlayerUsername().equals(game.getFirstPlayer().getUsername())) {
                state.getFirstPlayerHand().add(state.getDeck().removeFirst());
                if (!state.getDeck().isEmpty()) state.getSecondPlayerHand().add(state.getDeck().removeFirst());
            } else {
                state.getFirstPlayerHand().add(state.getDeck().removeFirst());
                if (!state.getDeck().isEmpty()) state.getSecondPlayerHand().add(state.getDeck().removeFirst());
            }
        }

        // reset table
        state.setFirstPlayerPlayedCard(null);
        state.setSecondPlayerPlayedCard(null);

    }

    private int determineWinner(Game game) {
        GameState state = game.getState();

        boolean c1Trump = state.getFirstPlayerPlayedCard().getSuit().equals(state.getTrumpCard().getSuit());
        boolean c2Trump = state.getSecondPlayerPlayedCard().getSuit().equals(state.getTrumpCard().getSuit());

        if (c1Trump && !c2Trump) return 1;
        if (c2Trump && !c1Trump) return 2;

        if (state.getFirstPlayerPlayedCard().getSuit() == state.getSecondPlayerPlayedCard().getSuit()) {
            return state.getFirstPlayerPlayedCard().getPoints() > state.getSecondPlayerPlayedCard().getPoints() ? 1 : 2;
        }

        return state.getInTurnPlayerUsername().equals(game.getSecondPlayer().getUsername()) ? 1 : 2;
    }

    private void checkTwentyForty(Game game, String playerInTurnUsername, Card playedCard) {
        if (!playerInTurnUsername.equals(game.getState().getFirstTurnPlayerUsername())) {
            return;
        }

        boolean isPlayerOne = game.getState().getInTurnPlayerUsername().equals(game.getFirstPlayer().getUsername());

        // Must play King OR Queen
        if (!(playedCard.getRank().name().equals("KING") || playedCard.getRank().name().equals("QUEEN"))) {
            return;
        }

        Suit suit = playedCard.getSuit();
        Suit trumpSuit = game.getState().getTrumpCard().getSuit();

        List<Card> hand = isPlayerOne ? game.getState().getFirstPlayerHand() : game.getState().getSecondPlayerHand();

        boolean hasMatchingPair = hand.stream().anyMatch(c ->
                c.getSuit() == suit &&
                        (
                                (playedCard.getRank().name().equals("KING") && c.getRank().name().equals("QUEEN")) ||
                                        (playedCard.getRank().name().equals("QUEEN") && c.getRank().name().equals("KING"))
                        )
        );

        if (!hasMatchingPair) return;

        // Bonus: 40 if trump, otherwise 20
        int bonus = (suit == trumpSuit) ? 40 : 20;

        if (isPlayerOne) {
            game.getState().setFirstPlayerScore(game.getState().getFirstPlayerScore() + bonus);
        } else {
            game.getState().setSecondPlayerScore(game.getState().getSecondPlayerScore() + bonus);
        }
    }
}
