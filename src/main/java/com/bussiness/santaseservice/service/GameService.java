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

        GameState state = new GameState();

        List<Card> deck = new ArrayList<>();
        for (Suit s : Suit.values()) {
            for (Rank r : Rank.values()) {
                deck.add(Card.builder().id(UUID.randomUUID()).suit(s).rank(r).build());
            }
        }
        Collections.shuffle(deck);
        state.setDeck(deck);

        // Deal 6 cards each
        for (int i = 0; i < 6; i++) state.getFirstPlayerHand().add(deck.removeFirst());
        for (int i = 0; i < 6; i++) state.getSecondPlayerHand().add(deck.removeFirst());

        state.setTrumpCard(deck.getFirst());

        // Trump
        //state.trumpCard = deck.getLast();

        Game game = Game.builder()
                .firstPlayer(firstPlayer)
                .secondPlayer(secondPlayer)
                .state(state)
                .build();
        gameStateRepo.save(state);
        return gameRepo.save(game);
    }

    @Transactional
    public GameState playCard(PlayCardRequest playCardRequest) {
        Game game = gameRepo.findById(playCardRequest.getGameId())
                .orElseThrow(() -> new RuntimeException("Game not found"));

        GameState state = game.getState();

        boolean isP1 = game.getFirstPlayer().getUsername().equals(playCardRequest.getUsername());
        boolean isP2 = game.getSecondPlayer().getUsername().equals(playCardRequest.getUsername());

        if (!isP1 && !isP2) {
            throw new RuntimeException("User not in this game");
        }

        // Check turn
        if (state.getIsPlayer1Turn() && !isP1) throw new RuntimeException("Not your turn");
        if (state.getIsPlayer1Turn() && isP2) throw new RuntimeException("Not your turn");

        // Remove card from player's hand
        if (isP1) {
            Card card = state.getFirstPlayerHand().stream()
                    .filter(c -> c.getId().equals(playCardRequest.getCardId()))
                    .findFirst()
                    .orElse(null);
            if (!state.getFirstPlayerHand().remove(card)) throw new RuntimeException("Card not in player's hand");
            state.setFirstPlayerPlayedCard(card);
        } else {
            Card card = state.getSecondPlayerHand().stream()
                    .filter(c -> c.getId().equals(playCardRequest.getCardId()))
                    .findFirst()
                    .orElse(null);
            if (!state.getSecondPlayerHand().remove(card)) throw new RuntimeException("Card not in player's hand");
            state.setSecondPlayerPlayedCard(card);
        }

        // If both players have played → evaluate trick
        if (state.getFirstPlayerPlayedCard() != null && state.getSecondPlayerPlayedCard() != null) {
            evaluateTrick(state);
        } else {
            // Only one played, switch turn
            state.setIsPlayer1Turn(Boolean.FALSE);
        }

        game.setState(state);
        gameRepo.save(game);
        return state;
    }

    private void evaluateTrick(GameState state) {
        Card firstPlayerCard = state.getFirstPlayerPlayedCard();
        Card secondPlayerCard = state.getSecondPlayerPlayedCard();

        int winner = determineWinner(firstPlayerCard, secondPlayerCard, state.getTrumpCard(), state.getIsPlayer1Turn());

        if (winner == 1) {
            state.setFirstPlayerScore(state.getFirstPlayerScore() + firstPlayerCard.getPoints() + secondPlayerCard.getPoints());
            state.setIsPlayer1Turn(Boolean.TRUE);
        } else {
            state.setSecondPlayerScore(state.getSecondPlayerScore() + firstPlayerCard.getPoints() + secondPlayerCard.getPoints());
            state.setIsPlayer1Turn(Boolean.FALSE);
        }

        // Draw new cards
        if (!state.getDeck().isEmpty()) {
            if (state.getIsPlayer1Turn()) {
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

    private int determineWinner(Card firstPlayerCard, Card secondPlayerCard, Card trumpCard, boolean isPlayer1Turn) {
        boolean c1Trump = firstPlayerCard.getSuit().equals(trumpCard.getSuit());
        boolean c2Trump = secondPlayerCard.getSuit().equals(trumpCard.getSuit());

        if (c1Trump && !c2Trump) return 1;
        if (c2Trump && !c1Trump) return 2;

        if (firstPlayerCard.getSuit() == secondPlayerCard.getSuit()) {
            return firstPlayerCard.getPoints() > secondPlayerCard.getPoints() ? 1 : 2;
        }

        return isPlayer1Turn ? 1 : 2;
    }
}
