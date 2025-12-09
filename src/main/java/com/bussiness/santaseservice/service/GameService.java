package com.bussiness.santaseservice.service;

import com.bussiness.santaseservice.enums.Rank;
import com.bussiness.santaseservice.enums.Suit;
import com.bussiness.santaseservice.model.Card;
import com.bussiness.santaseservice.model.Game;
import com.bussiness.santaseservice.model.GameState;
import com.bussiness.santaseservice.model.User;
import com.bussiness.santaseservice.model.request.CloseDeckRequest;
import com.bussiness.santaseservice.model.request.PlayCardRequest;
import com.bussiness.santaseservice.model.request.ReplaceCardRequest;
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
    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final GameStateRepository gameStateRepository;

    volatile Queue<String> matchQueue = new ConcurrentLinkedQueue<>();

    public GameState getGameState(Long gameId) {
        Game game = gameRepository.findById(gameId).orElseThrow(() -> new RuntimeException("Game id not found"));
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
        User firstPlayer = userRepository.findByUsername(firstPlayerUsername).orElseThrow();
        User secondPlayer = userRepository.findByUsername(secondPlayerUsername).orElseThrow();

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

        gameState.setTrumpCard(deck.getLast());

        Game game = Game.builder()
                .firstPlayer(firstPlayer)
                .secondPlayer(secondPlayer)
                .state(gameState)
                .build();
        gameStateRepository.save(gameState);
        return gameRepository.save(game);
    }

    @Transactional
    public GameState playCard(PlayCardRequest playCardRequest) {
        Game game = gameRepository.findById(playCardRequest.getGameId())
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
                    .orElseThrow(() -> new IllegalStateException("Card not found in player's hand"));
            removeCardFromHand(state.getFirstPlayerHand(), game.getFirstPlayer().getUsername(), card, game);
            state.setFirstPlayerPlayedCard(card);
            checkTwentyForty(game, game.getFirstPlayer().getUsername(), card);
        } else {
            Card card = state.getSecondPlayerHand().stream()
                    .filter(c -> c.getId().equals(playCardRequest.getCardId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Card not found in player's hand"));
            removeCardFromHand(state.getSecondPlayerHand(), game.getSecondPlayer().getUsername(), card, game);
            state.setSecondPlayerPlayedCard(card);
            checkTwentyForty(game, game.getSecondPlayer().getUsername(), card);
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
        gameRepository.save(game);
        return state;
    }

    public GameState closeDeck(CloseDeckRequest closeDeckRequest) {
        Game game = findGameForClosingOrRemoval(closeDeckRequest.getGameId(), closeDeckRequest.getUsername());

        GameState state = game.getState();
        state.getDeck().clear();

        return gameStateRepository.save(state);
    }

    public GameState replaceCard(ReplaceCardRequest replaceCardRequest) {
        Game game = findGameForClosingOrRemoval(replaceCardRequest.getGameId(), replaceCardRequest.getUsername());
        GameState state = game.getState();

        boolean isFirstPlayer = game.getFirstPlayer().getUsername().equals(replaceCardRequest.getUsername());
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

        return gameStateRepository.save(state);
    }

    private Game findGameForClosingOrRemoval(Long gameId, String username) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        GameState state = game.getState();

        if (!state.getFirstTurnPlayerUsername().equals(username)) {
            throw new RuntimeException("User is not first in turn in this game");
        }

        if (!state.getInTurnPlayerUsername().equals(username)) {
            throw new RuntimeException("Not your turn");
        }

        if (state.getDeck().size() == 2 || state.getDeck().size() == 12) {
            throw new RuntimeException("Deck must have more than 2 cards and less than 12 left");
        }

        return game;
    }

    private void removeCardFromHand(List<Card> playerCards, String playerUsername, Card cardForRemoval, Game game) {

        if (!playerCards.contains(cardForRemoval)) {
            throw new RuntimeException("Card not in player's hand");
        }

        // If deck still has cards → always allowed
        if (!game.getState().getDeck().isEmpty()) {
            playerCards.remove(cardForRemoval);
            return;
        }

        // First player always allowed
        if (playerUsername.equals(game.getState().getFirstTurnPlayerUsername())) {
            playerCards.remove(cardForRemoval);
            return;
        }

        // Determine opponent card
        Card opponentCard = playerUsername.equals(game.getFirstPlayer().getUsername())
                ? game.getState().getSecondPlayerPlayedCard()
                : game.getState().getFirstPlayerPlayedCard();

        Suit opponentSuit = opponentCard.getSuit();

        boolean hasSameSuit = playerCards.stream()
                .anyMatch(card -> card.getSuit() == opponentSuit);

        // --- CASE 1: Player has same suit as opponent ---
        if (hasSameSuit) {
            List<Card> playableCards = playerCards.stream()
                    .filter(c -> c.getSuit() == opponentSuit && c.getPoints() > opponentCard.getPoints())
                    .toList();

            if (playableCards.isEmpty()) {
                playableCards = playerCards.stream()
                        .filter(c -> c.getSuit() == opponentSuit)
                        .toList();
            }

            if (!playableCards.contains(cardForRemoval)) {
                throw new RuntimeException("Card cannot be played");
            }

            playerCards.remove(cardForRemoval);
            return;
        }

        // --- CASE 2: Player does NOT have same suit ---
        Card trumpCard = game.getState().getTrumpCard();
        Suit trumpSuit = trumpCard.getSuit();

        // Opponent card is trump - you may play anything
        if (opponentSuit == trumpSuit) {
            playerCards.remove(cardForRemoval);
            return;
        }

        // Otherwise check if player has trump cards
        boolean hasTrump = playerCards.stream()
                .anyMatch(c -> c.getSuit() == trumpSuit);

        if (hasTrump && cardForRemoval.getSuit() != trumpSuit) {
            throw new RuntimeException("Card cannot be played");
        }

        playerCards.remove(cardForRemoval);
    }

    private void evaluateTrick(Game game) {
        GameState state = game.getState();
        Card firstPlayerCard = state.getFirstPlayerPlayedCard();
        Card secondPlayerCard = state.getSecondPlayerPlayedCard();

        int winner = determineWinner(game);

        if (winner == 1) {
            state.setFirstPlayerScore(state.getFirstPlayerScore() + firstPlayerCard.getPoints() + secondPlayerCard.getPoints());
            state.setInTurnPlayerUsername(game.getFirstPlayer().getUsername());
            state.setFirstTurnPlayerUsername(game.getFirstPlayer().getUsername());
        } else {
            state.setSecondPlayerScore(state.getSecondPlayerScore() + firstPlayerCard.getPoints() + secondPlayerCard.getPoints());
            state.setInTurnPlayerUsername(game.getSecondPlayer().getUsername());
            state.setFirstTurnPlayerUsername(game.getSecondPlayer().getUsername());
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
        if (!playerInTurnUsername.equals(game.getState().getFirstTurnPlayerUsername()) ||
                game.getState().getDeck().size() == 12) {
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
