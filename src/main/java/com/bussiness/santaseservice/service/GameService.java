package com.bussiness.santaseservice.service;

import com.bussiness.santaseservice.enums.Rank;
import com.bussiness.santaseservice.enums.Suit;
import com.bussiness.santaseservice.model.Card;
import com.bussiness.santaseservice.model.Game;
import com.bussiness.santaseservice.model.GameState;
import com.bussiness.santaseservice.model.User;
import com.bussiness.santaseservice.model.request.CloseDeckRequest;
import com.bussiness.santaseservice.model.request.FinishDealRequest;
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

    volatile Queue<User> matchQueue = new ConcurrentLinkedQueue<>();

    public GameState getGameState(Long gameId) {
        Game game = gameRepository.findById(gameId).orElseThrow(() -> new RuntimeException("Game id not found"));
        return game.getState();
    }

    @Transactional
    public synchronized Game searchGame(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        User waitingUser = matchQueue.poll();

        if (waitingUser == null) {
            matchQueue.offer(user);
            return null;
        } else {
            return startGame(user, waitingUser);
        }
    }

    @Transactional
    public Game startGame(User firstPlayer, User secondPlayer) {
        GameState gameState = GameState.builder().build();

        Game game = Game.builder()
                .firstPlayer(firstPlayer)
                .secondPlayer(secondPlayer)
                .state(gameState)
                .build();

        prepareNewState(game, null);
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

    @Transactional
    public GameState closeDeck(CloseDeckRequest closeDeckRequest) {
        Game game = findGameForClosingOrRemoval(closeDeckRequest.getGameId(), closeDeckRequest.getUsername());

        GameState state = game.getState();
        state.getDeck().clear();
        state.setClosedByUsername(closeDeckRequest.getUsername());

        return gameStateRepository.save(state);
    }

    @Transactional
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

    @Transactional
    public GameState finishDeal(FinishDealRequest req) {
        Game game = gameRepository.findById(req.getGameId())
                .orElseThrow(() -> new RuntimeException("Game not found"));

        GameState state = game.getState();

        String user = req.getUsername();

        if (!state.getFirstTurnPlayerUsername().equals(user))
            throw new RuntimeException("User is not first in turn in this game");

        if (!state.getInTurnPlayerUsername().equals(user))
            throw new RuntimeException("Not your turn");

        boolean isFirst = game.getFirstPlayer().getUsername().equals(user);

        int playerScore = isFirst ? state.getFirstPlayerScore() : state.getSecondPlayerScore();
        int opponentScore = isFirst ? state.getSecondPlayerScore() : state.getFirstPlayerScore();
        boolean opponentBlanked = isFirst ? state.getIsSecondPlayerBlanked()
                : state.getIsFirstPlayerBlanked();

        // Determine round points
        int pointsAwarded;

        if (playerScore >= 66) {
            // Player wins normally
            pointsAwarded = opponentBlanked ? 3 : (opponentScore < 33 ? 2 : 1);
        } else {
            // Opponent wins (player failed to reach 66)
            pointsAwarded = 3;
            isFirst = !isFirst;  // flip winner
        }

        // Apply result
        if (isFirst) {
            game.setFirstPlayerResult(game.getFirstPlayerResult() + pointsAwarded);
            prepareNewState(game, game.getFirstPlayer()); // winner = first
        } else {
            game.setSecondPlayerResult(game.getSecondPlayerResult() + pointsAwarded);
            prepareNewState(game, game.getSecondPlayer()); // winner = second
        }

        return state;
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
        User first = game.getFirstPlayer();
        User second = game.getSecondPlayer();

        Card firstCard = state.getFirstPlayerPlayedCard();
        Card secondCard = state.getSecondPlayerPlayedCard();

        User winner = determineWinner(game);
        boolean firstWins = winner.equals(first);

        // --- Award trick points ---
        int trickPoints = firstCard.getPoints() + secondCard.getPoints();
        if (firstWins) {
            state.setFirstPlayerScore(state.getFirstPlayerScore() + trickPoints);
        } else {
            state.setSecondPlayerScore(state.getSecondPlayerScore() + trickPoints);
        }

        // --- Update turn ownership ---
        String winnerName = winner.getUsername();
        state.setInTurnPlayerUsername(winnerName);
        state.setFirstTurnPlayerUsername(winnerName);

        // --- Reset blank flags only for the winner ---
        if (firstWins) {
            if (state.getIsFirstPlayerBlanked()) state.setIsFirstPlayerBlanked(false);
        } else {
            if (state.getIsSecondPlayerBlanked()) state.setIsSecondPlayerBlanked(false);
        }

        // --- Draw cards ---
        drawCards(state, winnerName, first);

        // --- End of game scoring ---
        if (isLastCardPlayed(state)) {
            applyEndOfGameScore(game, state, winner, first, second);
            prepareNewState(game, winner);
        }

        // --- Cleanup ---
        state.setFirstPlayerPlayedCard(null);
        state.setSecondPlayerPlayedCard(null);
    }

    private void drawCards(GameState state, String winnerName, User first) {
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

    private void applyEndOfGameScore(Game game, GameState state, User winner, User first, User second) {
        boolean winnerIsFirst = winner.equals(first);
        boolean closedByOther =
                state.getClosedByUsername() != null &&
                        !state.getClosedByUsername().equals(winnerIsFirst ? second.getUsername() : first.getUsername());

        int bonus;

        if (closedByOther) {
            bonus = 3;
        } else {
            int loserScore = winnerIsFirst ? state.getSecondPlayerScore() : state.getFirstPlayerScore();
            boolean loserBlank = winnerIsFirst ? state.getIsSecondPlayerBlanked() : state.getIsFirstPlayerBlanked();

            bonus = loserBlank ? 3 : (loserScore < 33 ? 2 : 1);
        }

        if (winnerIsFirst) {
            game.setFirstPlayerResult(game.getFirstPlayerResult() + bonus);
        } else {
            game.setSecondPlayerResult(game.getSecondPlayerResult() + bonus);
        }
    }

    private void prepareNewState(Game game, User winner) {
        game.getState().setDeck(getNewDeck());

        if (winner == null || winner.equals(game.getSecondPlayer())) {
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

    private List<Card> getNewDeck() {
        List<Card> deck = new ArrayList<>();
        for (Suit s : Suit.values()) {
            for (Rank r : Rank.values()) {
                deck.add(Card.builder().id(UUID.randomUUID()).suit(s).rank(r).build());
            }
        }
        Collections.shuffle(deck);
        return deck;
    }

    private boolean isLastCardPlayed(GameState state) {
        return state.getDeck().isEmpty() && state.getFirstPlayerHand().isEmpty() && state.getSecondPlayerHand().isEmpty();
    }

    private User determineWinner(Game game) {
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
