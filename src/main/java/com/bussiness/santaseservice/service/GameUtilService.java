package com.bussiness.santaseservice.service;

import com.bussiness.santaseservice.enums.Rank;
import com.bussiness.santaseservice.enums.Suit;
import com.bussiness.santaseservice.model.Card;
import com.bussiness.santaseservice.model.Game;
import com.bussiness.santaseservice.model.GameState;
import com.bussiness.santaseservice.model.User;
import com.bussiness.santaseservice.repository.GameRepository;
import com.bussiness.santaseservice.repository.GameStateRepository;
import com.bussiness.santaseservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class GameUtilService {
    private final GameRepository gameRepository;
    private final GameStateRepository gameStateRepository;
    private final UserRepository userRepository;
    private final GameWebSocketService gameWebSocketService;

    @Transactional
    protected Game startGame(User firstPlayer, User secondPlayer) {
        GameState gameState = GameState.builder().build();

        Game game = Game.builder()
                .firstPlayer(firstPlayer)
                .secondPlayer(secondPlayer)
                .state(gameState)
                .firstPlayerResult(0)
                .secondPlayerResult(0)
                .build();

        prepareNewState(game, null);
        return gameRepository.save(game);
    }

    protected Game findGameById(UUID id) {
        return gameRepository.findByIdAndWinnerIsNull(id)
                .orElseThrow(() -> new RuntimeException("Game not found or finished"));
    }

    protected void saveGame(Game game) {
        gameRepository.save(game);
    }

    protected void saveGameState(GameState gameState) {
        gameStateRepository.save(gameState);
    }

    protected User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    protected Game findGameForClosingOrRemoval(UUID gameId, String username) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        GameState state = game.getState();

        if (!state.getFirstTurnPlayerUsername().equals(username)) {
            throw new RuntimeException("User is not first in turn in this game");
        }

        if (!state.getInTurnPlayerUsername().equals(username)) {
            throw new RuntimeException("Not your turn");
        }

        if (state.getDeck().size() <= 2 || state.getDeck().size() == 12) {
            throw new IllegalStateException("Deck size must be greater than 2 and less than 12");
        }

        return game;
    }

    protected void removeCardFromHand(List<Card> playerCards, String playerUsername, Card cardForRemoval, Game game) {

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

    protected void evaluateTrick(Game game) {
        GameState state = game.getState();
        User firstPlayer = game.getFirstPlayer();

        Card firstCard = state.getFirstPlayerPlayedCard();
        Card secondCard = state.getSecondPlayerPlayedCard();

        User trickWinner = determineWinner(game);
        boolean firstWins = trickWinner.equals(firstPlayer);

        // --- Award trick points ---
        int trickPoints = firstCard.getPoints() + secondCard.getPoints();
        if (firstWins) {
            state.setFirstPlayerScore(state.getFirstPlayerScore() + trickPoints);
        } else {
            state.setSecondPlayerScore(state.getSecondPlayerScore() + trickPoints);
        }

        // --- Update turn ownership ---
        String winnerName = trickWinner.getUsername();
        state.setInTurnPlayerUsername(winnerName);
        state.setFirstTurnPlayerUsername(winnerName);

        // --- Reset blank flags only for the winner ---
        if (firstWins) {
            if (state.getIsFirstPlayerBlanked()) state.setIsFirstPlayerBlanked(false);
        } else {
            if (state.getIsSecondPlayerBlanked()) state.setIsSecondPlayerBlanked(false);
        }

        // --- Draw cards ---
        drawCards(state, winnerName, firstPlayer);

        // --- End of game scoring ---
        if (isLastCardPlayed(state)) {
            gameWebSocketService.updateGameState(game, game.getFirstPlayer().getUsername(),
                    trickWinner.getUsername(), state.getFirstPlayerScore(), state.getSecondPlayerScore());

            gameWebSocketService.updateGameState(game, game.getSecondPlayer().getUsername(),
                    trickWinner.getUsername(), state.getFirstPlayerScore(), state.getSecondPlayerScore());
            applyEndOfGameScore(game, state, trickWinner, firstPlayer, game.getSecondPlayer());
            prepareNewState(game, trickWinner);
        }

        // --- Cleanup ---
        state.setFirstPlayerPlayedCard(null);
        state.setSecondPlayerPlayedCard(null);
    }

    protected void drawCards(GameState state, String winnerName, User first) {
        if (state.getDeck().isEmpty()) return;

        // Player in turn draws first
        boolean firstInTurn = winnerName.equals(first.getUsername());

        if (firstInTurn) {
            state.getFirstPlayerHand().add(state.getDeck().removeFirst());
        } else {
            state.getSecondPlayerHand().add(state.getDeck().removeFirst());
        }

        // Other player draws second (if deck not empty)
        if (!state.getDeck().isEmpty()) {
            if (firstInTurn) {
                state.getSecondPlayerHand().add(state.getDeck().removeFirst());
            } else {
                state.getFirstPlayerHand().add(state.getDeck().removeFirst());
            }
        }
    }

    protected void applyEndOfGameScore(Game game, GameState state, User finalTrickWinner, User firstPlayer, User secondPlayer) {
        User dealWinner;
        int bonusPoints;

        // 1. Check if the game was closed
        if (state.isClosed()) {

            // --- Closed Game Scenario (New Rule Set) ---

            // Identify the closer and the non-closer
            boolean closerIsFirst = state.getClosedByUsername().equals(firstPlayer.getUsername());
            User closer = closerIsFirst ? firstPlayer : secondPlayer;
            User nonCloser = closerIsFirst ? secondPlayer : firstPlayer;
            int closerResult = closerIsFirst ?
                    game.getState().getFirstPlayerScore() : game.getState().getSecondPlayerScore();

            // Rule: The closer wins points if they reached 66 or more
            if (closerResult >= 66) {
                // Closer Wins Deal (Successful close)
                dealWinner = closer;

                // Points based on the non-closer's score (standard 1, 2, or 3)
                bonusPoints = calculateStandardBonus(
                        closerIsFirst ? game.getState().getSecondPlayerScore() : game.getState().getFirstPlayerScore(),
                        closerIsFirst ? game.getState().getIsSecondPlayerBlanked() : game.getState().getIsFirstPlayerBlanked());

            } else {
                // Closer Fails (Did not reach 66 points)
                dealWinner = nonCloser;

                // Rule: The other player (non-closer) wins 3 points (Penalty)
                bonusPoints = 3;
            }

        } else {

            // --- Open Game Scenario (Played to 66 or last trick - Original Rule Set) ---

            // The player who won the deal is the winner
            dealWinner = finalTrickWinner;
            boolean winnerIsFirst = dealWinner.getUsername().equals(firstPlayer.getUsername());

            // Points based on the loser's score (Standard Santase Scoring)
            bonusPoints = calculateStandardBonus(
                    winnerIsFirst ? game.getState().getSecondPlayerScore() : game.getState().getFirstPlayerScore(),
                    winnerIsFirst ? game.getState().getIsSecondPlayerBlanked() : game.getState().getIsFirstPlayerBlanked());
        }

        // --- Apply Scores to Game Result ---
        if (dealWinner.equals(firstPlayer)) {
            game.setFirstPlayerResult(game.getFirstPlayerResult() + bonusPoints);
        } else {
            game.setSecondPlayerResult(game.getSecondPlayerResult() + bonusPoints);
        }
    }

    private int calculateStandardBonus(int loserScore, boolean loserBlank) {
        if (loserBlank) {
            return 3; // Loser was blanked (had 0 tricks/score)
        } else if (loserScore < 33) {
            return 2; // Loser scored less than 33
        } else {
            return 1; // Loser scored 33 or more
        }
    }

    protected void prepareNewState(Game game, User trickWinner) {
        int firstPlayerResult = game.getFirstPlayerResult();
        int secondPlayerResult = game.getSecondPlayerResult();

        int difference = Math.abs(firstPlayerResult - secondPlayerResult);

        if ((firstPlayerResult >= 11 || secondPlayerResult >= 11) && difference >= 2) {
            if (firstPlayerResult > secondPlayerResult) {
                game.setWinner(game.getFirstPlayer());
            } else {
                game.setWinner(game.getSecondPlayer());
            }
            return;
        }

        game.getState().setDeck(getNewDeck());

        if (trickWinner == null || trickWinner.equals(game.getSecondPlayer())) {
            game.getState().setFirstTurnPlayerUsername(game.getFirstPlayer().getUsername());
            game.getState().setInTurnPlayerUsername(game.getFirstPlayer().getUsername());
        } else {
            game.getState().setFirstTurnPlayerUsername(game.getSecondPlayer().getUsername());
            game.getState().setInTurnPlayerUsername(game.getSecondPlayer().getUsername());
        }

        game.getState().setFirstPlayerHand(new ArrayList<>());
        game.getState().setSecondPlayerHand(new ArrayList<>());
        game.getState().setFirstPlayerScore(0);
        game.getState().setSecondPlayerScore(0);
        game.getState().setIsFirstPlayerBlanked(true);
        game.getState().setIsSecondPlayerBlanked(true);
        game.getState().setClosedByUsername(null);

        // Deal 6 cards each
        for (int i = 0; i < 6; i++) game.getState().getFirstPlayerHand().add(game.getState().getDeck().removeFirst());
        for (int i = 0; i < 6; i++) game.getState().getSecondPlayerHand().add(game.getState().getDeck().removeFirst());

        game.getState().setTrumpCard(game.getState().getDeck().getLast());
    }

    protected List<Card> getNewDeck() {
        List<Card> deck = new ArrayList<>();
        for (Suit s : Suit.values()) {
            for (Rank r : Rank.values()) {
                deck.add(Card.builder().id(UUID.randomUUID()).suit(s).rank(r).build());
            }
        }
        Collections.shuffle(deck);
        return deck;
    }

    protected boolean isLastCardPlayed(GameState state) {
        return state.getDeck().isEmpty() && state.getFirstPlayerHand().isEmpty() && state.getSecondPlayerHand().isEmpty();
    }

    protected User determineWinner(Game game) {
        GameState state = game.getState();

        boolean c1Trump = state.getFirstPlayerPlayedCard().getSuit().equals(state.getTrumpCard().getSuit());
        boolean c2Trump = state.getSecondPlayerPlayedCard().getSuit().equals(state.getTrumpCard().getSuit());

        if (c1Trump && !c2Trump) return game.getFirstPlayer();
        if (c2Trump && !c1Trump) return game.getSecondPlayer();

        if (state.getFirstPlayerPlayedCard().getSuit() == state.getSecondPlayerPlayedCard().getSuit()) {
            return state.getFirstPlayerPlayedCard().getPoints() > state.getSecondPlayerPlayedCard().getPoints()
                    ? game.getFirstPlayer() : game.getSecondPlayer();
        }

        return state.getInTurnPlayerUsername().equals(game.getSecondPlayer().getUsername())
                ? game.getFirstPlayer() : game.getSecondPlayer();
    }

    protected void checkTwentyForty(Game game, String playerInTurnUsername, Card playedCard) {
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
