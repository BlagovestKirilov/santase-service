package bg.deck.santaseservice.service;

import bg.deck.santaseservice.enums.Rank;
import bg.deck.santaseservice.enums.Suit;
import bg.deck.santaseservice.model.Card;
import bg.deck.santaseservice.model.Game;
import bg.deck.santaseservice.model.GameState;
import bg.deck.santaseservice.model.Player;
import bg.deck.santaseservice.model.response.SearchGameResponse;
import bg.deck.santaseservice.repository.GameRepository;
import bg.deck.santaseservice.repository.GameStateRepository;
import bg.deck.santaseservice.repository.PlayerRepository;
import bg.deck.santaseservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class GameUtilService {
    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;
    private final GameStateRepository gameStateRepository;
    private final UserRepository userRepository;
    private final GameWebSocketService gameWebSocketService;

    @Transactional
    protected Game startGame(Player firstPlayer, Player secondPlayer) {
        GameState gameState = GameState.builder().build();
        firstPlayer.setResult(0);
        secondPlayer.setResult(0);

        Game game = Game.builder()
                .firstPlayer(firstPlayer)
                .secondPlayer(secondPlayer)
                .state(gameState)
                .build();

        prepareNewState(game, null);
        return gameRepository.save(game);
    }

    protected Game findGameByUsername(String username) {
        return gameRepository.findActiveGamesByUsername(username)
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No active game found for this user"));
    }

    protected void saveGame(Game game) {
        gameRepository.save(game);
    }

    protected void saveGameState(GameState gameState) {
        gameStateRepository.save(gameState);
    }

    protected String getUsername() {
        return Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
    }

    protected boolean checkIfUserExistsAndIsAvailable(String username) {
        if (userRepository.existsByUsername(username)) {
            Optional<UUID> gameId = userRepository.findActiveGameIdByUsername(username);

            if (gameId.isPresent()) {
                gameWebSocketService.notifyGameSearch(username, SearchGameResponse.started(gameId.get()));
                return false;
            } else {
                return true;
            }
        } else {
            throw new RuntimeException("User not found");
        }
    }

    protected Player findPlayerByUsername(String username) {
        return playerRepository.findByUserUsername(username)
                .orElseThrow(() -> new RuntimeException("Player not found"));
    }

    protected Game findGameForClosingOrRemoval(String username) {
        Game game = findGameByUsername(username);

        Player player = game.getPlayerByUsername(username);

        GameState state = game.getState();

        if (!state.getFirstTurnPlayer().equals(player)) {
            throw new RuntimeException("User is not first in turn in this game");
        }

        if (!state.getInTurnPlayer().equals(player)) {
            throw new RuntimeException("Not your turn");
        }

        if (state.getDeck().size() <= 2 || state.getDeck().size() == 12) {
            throw new IllegalStateException("Deck size must be greater than 2 and less than 12");
        }

        return game;
    }

    protected void removeCardFromHand(Game game, Player player, Card cardForRemoval) {
        List<Card> playerCards = player.getHand();

        if (!playerCards.contains(cardForRemoval)) {
            throw new RuntimeException("Card not in player's hand");
        }

        // If deck still has cards → always allowed
        if (!game.getState().getDeck().isEmpty()) {
            playerCards.remove(cardForRemoval);
            return;
        }

        // First player always allowed
        if (player.equals(game.getState().getFirstTurnPlayer())) {
            playerCards.remove(cardForRemoval);
            return;
        }

        Card opponentPlayerCard = game.getOpponent(player).getPlayedCard();

        Suit opponentSuit = opponentPlayerCard.getSuit();

        boolean hasSameSuit = playerCards.stream()
                .anyMatch(card -> card.getSuit() == opponentSuit);

        // --- CASE 1: Player has same suit as opponent ---
        if (hasSameSuit) {
            List<Card> playableCards = playerCards.stream()
                    .filter(c -> c.getSuit() == opponentSuit && c.getPoints() > opponentPlayerCard.getPoints())
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
        Player firstPlayer = game.getFirstPlayer();
        Player secondPlayer = game.getSecondPlayer();

        Player trickWinner = determineWinner(game);

        // --- Award trick points ---
        int trickPoints = firstPlayer.getPlayedCard().getPoints() + secondPlayer.getPlayedCard().getPoints();
        trickWinner.setScore(trickWinner.getScore() + trickPoints);

        state.setInTurnPlayer(trickWinner);
        state.setFirstTurnPlayer(trickWinner);

        if (trickWinner.getIsBlanked()) {
            trickWinner.setIsBlanked(false);
        }

        // --- Draw cards ---
        drawCards(game, trickWinner);

        // --- End of game scoring ---
        if (isLastCardPlayed(game)) {
            gameWebSocketService.updateGameState(game, game.getFirstPlayer().getUsername(),
                    trickWinner.getUsername(), game.getFirstPlayer().getScore(), game.getSecondPlayer().getScore());

            gameWebSocketService.updateGameState(game, game.getSecondPlayer().getUsername(),
                    trickWinner.getUsername(), game.getFirstPlayer().getScore(), game.getSecondPlayer().getScore());
            applyEndOfGameScore(game, trickWinner);
            prepareNewState(game, trickWinner);
        }

        // --- Cleanup ---
        firstPlayer.setPlayedCard(null);
        secondPlayer.setPlayedCard(null);
    }

    protected void drawCards(Game game, Player trickWinner) {
        if (game.getState().getDeck().isEmpty()) return;

        trickWinner.getHand().add(game.getState().getDeck().removeFirst());
        game.getOpponent(trickWinner).getHand().add(game.getState().getDeck().removeFirst());
    }

    protected void applyEndOfGameScore(Game game, Player trickWinner) {
        GameState state = game.getState();

        Player dealWinner;
        int bonusPoints;

        if (state.isClosed()) {
            Player closer = state.getClosedByPlayer();
            Player opponent = game.getOpponent(closer);

            if (closer.getScore() >= 66) {
                // Successful close
                dealWinner = closer;
                bonusPoints = calculateStandardBonus(opponent.getScore(), opponent.getIsBlanked());
            } else {
                // Failed close → penalty
                dealWinner = opponent;
                bonusPoints = 3;
            }

        } else {
            // Open game (normal end)
            dealWinner = trickWinner;
            Player loser = game.getOpponent(trickWinner);
            bonusPoints = calculateStandardBonus(loser.getScore(), loser.getIsBlanked());
        }

        dealWinner.setResult(dealWinner.getResult() + bonusPoints);
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

    protected void prepareNewState(Game game, Player trickWinner) {
        int firstPlayerResult = game.getFirstPlayer().getResult();
        int secondPlayerResult = game.getSecondPlayer().getResult();

        int difference = Math.abs(firstPlayerResult - secondPlayerResult);

        if ((firstPlayerResult >= 3 || secondPlayerResult >= 3) && difference >= 2) {
            if (firstPlayerResult > secondPlayerResult) {
                game.setWinner(game.getFirstPlayer());
            } else {
                game.setWinner(game.getSecondPlayer());
            }
            return;
        }

        game.getState().setDeck(getNewDeck());

        if (trickWinner == null || trickWinner.equals(game.getSecondPlayer())) {
            game.getState().setFirstTurnPlayer(game.getFirstPlayer());
            game.getState().setInTurnPlayer(game.getFirstPlayer());
        } else {
            game.getState().setFirstTurnPlayer(game.getSecondPlayer());
            game.getState().setInTurnPlayer(game.getSecondPlayer());
        }

        game.getFirstPlayer().setHand(new ArrayList<>());
        game.getSecondPlayer().setHand(new ArrayList<>());
        game.getFirstPlayer().setScore(0);
        game.getSecondPlayer().setScore(0);
        game.getFirstPlayer().setIsBlanked(true);
        game.getSecondPlayer().setIsBlanked(true);
        game.getState().setClosedByPlayer(null);

        // Deal 6 cards each
        for (int i = 0; i < 3; i++) game.getFirstPlayer().getHand().add(game.getState().getDeck().removeFirst());
        for (int i = 0; i < 3; i++) game.getSecondPlayer().getHand().add(game.getState().getDeck().removeFirst());
        for (int i = 0; i < 3; i++) game.getFirstPlayer().getHand().add(game.getState().getDeck().removeFirst());
        for (int i = 0; i < 3; i++) game.getSecondPlayer().getHand().add(game.getState().getDeck().removeFirst());

        game.getState().setTrumpCard(game.getState().getDeck().getLast());
    }

    protected List<Card> getNewDeck() {
        List<Card> deck = new ArrayList<>();
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                deck.add(Card.builder()
                        .id(UUID.randomUUID())
                        .suit(suit)
                        .rank(rank)
                        .isPlayable(true)
                        .build());
            }
        }
        Collections.shuffle(deck);
        return deck;
    }

    protected boolean isLastCardPlayed(Game game) {
        return game.getState().getDeck().isEmpty()
                && game.getFirstPlayer().getHand().isEmpty()
                && game.getSecondPlayer().getHand().isEmpty();
    }

    protected Player determineWinner(Game game) {
        GameState state = game.getState();
        Card firstPlayerCard = game.getFirstPlayer().getPlayedCard();
        Card secondPlayerCard = game.getSecondPlayer().getPlayedCard();

        boolean isFirstPlayerCardTrump = firstPlayerCard.getSuit().equals(state.getTrumpCard().getSuit());
        boolean isSecondPlayerCardTrump = secondPlayerCard.getSuit().equals(state.getTrumpCard().getSuit());

        if (isFirstPlayerCardTrump && !isSecondPlayerCardTrump) return game.getFirstPlayer();
        if (isSecondPlayerCardTrump && !isFirstPlayerCardTrump) return game.getSecondPlayer();

        if (firstPlayerCard.getSuit() == secondPlayerCard.getSuit()) {
            return firstPlayerCard.getPoints() > secondPlayerCard.getPoints()
                    ? game.getFirstPlayer() : game.getSecondPlayer();
        }

        return game.getState().getFirstTurnPlayer();
    }

    protected void checkTwentyForty(Game game, Player player, Card playedCard) {
        if (game.getState().getDeck().size() == 12) {
            return;
        }

        // Must play King OR Queen
        if (!(playedCard.getRank().name().equals("KING") || playedCard.getRank().name().equals("QUEEN"))) {
            return;
        }

        Suit suit = playedCard.getSuit();
        List<Card> hand = player.getHand();

        Card matchingPartner = hand.stream()
                .filter(c -> c.getSuit() == suit &&
                        ((playedCard.getRank().name().equals("KING") && c.getRank().name().equals("QUEEN")) ||
                                (playedCard.getRank().name().equals("QUEEN") && c.getRank().name().equals("KING"))))
                .findFirst()
                .orElse(null);

        if (matchingPartner == null) return;

        Suit trumpSuit = game.getState().getTrumpCard().getSuit();

        // Bonus: 40 if trump, otherwise 20
        int bonus = (suit == trumpSuit) ? 40 : 20;

        player.setBonus(bonus);
        player.setScore(player.getScore() + bonus);

        hand.forEach(card -> {
            if (!card.equals(playedCard) && !card.equals(matchingPartner)) {
                card.setIsPlayable(false);
            }
        });
    }
}
