package bg.deck.santaseservice;

import bg.deck.santaseservice.enums.card.Rank;
import bg.deck.santaseservice.enums.card.Suit;
import bg.deck.santaseservice.exception.NotInTurnException;
import bg.deck.santaseservice.model.Card;
import bg.deck.santaseservice.model.Game;
import bg.deck.santaseservice.model.GameState;
import bg.deck.santaseservice.model.Player;
import bg.deck.santaseservice.model.User;
import bg.deck.santaseservice.model.request.CardRequest;
import bg.deck.santaseservice.model.response.SearchGameResponse;
import bg.deck.santaseservice.service.GameService;
import bg.deck.santaseservice.service.GameUtilService;
import bg.deck.santaseservice.service.WebSocketService;
import bg.deck.santaseservice.service.WebSocketUtilService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    private final String p1Name = "Alice";
    private final String p2Name = "Bob";
    @Mock
    private WebSocketUtilService webSocketUtilService;
    @Mock
    private WebSocketService webSocketService;
    @Mock
    private GameUtilService gameUtilService;
    @InjectMocks
    private GameService gameService;
    private Player p1;
    private Player p2;
    private Game game;
    private GameState state;

    @BeforeEach
    void setUp() {
        p1 = createPlayer(p1Name);
        p2 = createPlayer(p2Name);
        state = GameState.builder()
                .inTurnPlayer(p1)
                .firstTurnPlayer(p1)
                .deck(new ArrayList<>())
                .trumpCard(new Card(UUID.randomUUID(), Suit.HEARTS, Rank.ACE, true, false))
                .build();

        game = Game.builder()
                .firstPlayer(p1)
                .secondPlayer(p2)
                .state(state)
                .build();
    }

    private Player createPlayer(String name) {
        User user = new User();
        user.setUsername(name);
        return Player.builder().user(user).hand(new ArrayList<>()).score(0).result(0).build();
    }

    @Nested
    @DisplayName("Matchmaking & Search Tests")
    class SearchTests {
        @Test
        void searchGame_WhenQueueEmpty_AddsUserToQueue() {
            when(gameUtilService.getUsername()).thenReturn(p1Name);
            when(gameUtilService.checkIfUserExistsAndIsAvailable(p1Name)).thenReturn(true);

            gameService.searchGame();

            verify(webSocketService).notifyGameSearch(eq(p1Name), any(SearchGameResponse.class));
            verify(gameUtilService, never()).startGame(any(), any());
        }

        @Test
        void searchGame_WhenPlayerInQueue_StartsNewGame() {
            // First player enters queue
            when(gameUtilService.getUsername()).thenReturn(p1Name);
            when(gameUtilService.checkIfUserExistsAndIsAvailable(p1Name)).thenReturn(true);
            gameService.searchGame();

            // Second player enters queue
            reset(webSocketUtilService);
            when(gameUtilService.getUsername()).thenReturn(p2Name);
            when(gameUtilService.checkIfUserExistsAndIsAvailable(p2Name)).thenReturn(true);
            when(gameUtilService.findPlayerByUsername(p2Name)).thenReturn(p2);
            when(gameUtilService.findPlayerByUsername(p1Name)).thenReturn(p1);
            when(gameUtilService.startGame(p2, p1)).thenReturn(game);

            gameService.searchGame();

            verify(gameUtilService).startGame(any(), any());
            verify(webSocketService).notifyGameSearch(anyList(), any(SearchGameResponse.class));
        }
    }


    @Nested
    @DisplayName("Gameplay Action Tests")
    class GameplayTests {

        @Test
        void playCard_Success_TurnSwitches() {
            Card card = new Card(UUID.randomUUID(), Suit.CLUBS, Rank.TEN, true, false);
            p1.getHand().add(card);
            CardRequest request = new CardRequest(card.getId());

            when(gameUtilService.getUsername()).thenReturn(p1Name);
            when(gameUtilService.findGameByUsername(p1Name)).thenReturn(game);

            gameService.playCard(request);

            assertThat(p1.getPlayedCard()).isEqualTo(card);
            assertThat(state.getInTurnPlayer()).isEqualTo(p2);
            verify(gameUtilService).removeCardFromHand(game, p1, card);
            verify(gameUtilService).saveGame(game);
        }

        @Test
        void playCard_WhenBothPlayed_TriggersEvaluation() {
            Card p2Card = new Card(UUID.randomUUID(), Suit.HEARTS, Rank.KING, true, false);
            p2.setPlayedCard(p2Card); // Bob already played

            Card p1Card = new Card(UUID.randomUUID(), Suit.HEARTS, Rank.TEN, true, false);
            p1.getHand().add(p1Card);

            when(gameUtilService.getUsername()).thenReturn(p1Name);
            when(gameUtilService.findGameByUsername(p1Name)).thenReturn(game);

            gameService.playCard(new CardRequest(p1Card.getId()));

            verify(gameUtilService).evaluateTrick(game);
        }

        @Test
        void playCard_WrongTurn_ThrowsException() {
            // 1. Arrange: Setup state and inputs outside the assertion
            state.setInTurnPlayer(p2);
            when(gameUtilService.getUsername()).thenReturn(p1Name);
            when(gameUtilService.findGameByUsername(p1Name)).thenReturn(game);

            UUID randomId = UUID.randomUUID();
            CardRequest request = new CardRequest(randomId);

            // 2. Act & Assert: Only the specific service call is inside the lambda
            assertThatThrownBy(() -> gameService.playCard(request))
                    .isInstanceOf(NotInTurnException.class);
        }
    }

    @Nested
    @DisplayName("Marriage & Special Action Tests")
    class SpecialActionTests {

        @Test
        void announceCombination_Success() {
            Card king = new Card(UUID.randomUUID(), Suit.HEARTS, Rank.KING, true, false);
            p1.getHand().add(king);

            when(gameUtilService.getUsername()).thenReturn(p1Name);
            when(gameUtilService.findGameByUsername(p1Name)).thenReturn(game);
            when(gameUtilService.checkTwentyForty(game, p1, king)).thenReturn(true);

            gameService.announceCombination(new CardRequest(king.getId()));

            verify(webSocketUtilService).updateGameState(game);
            assertThat(p1.getBonus()).isNull(); // Reset after successful logic
        }

        @Test
        void replaceCard_Success() {
            // 1. Set up the specific cards needed for the Santase "9 of Trumps" replacement rule
            Card aceOfHearts = new Card(UUID.randomUUID(), Suit.HEARTS, Rank.ACE, true, false);
            Card nineOfHearts = new Card(UUID.randomUUID(), Suit.HEARTS, Rank.NINE, true, false);

            // The trump card is the Ace, the player has the Nine
            state.setTrumpCard(aceOfHearts);
            state.getDeck().add(new Card()); // Just to have something in deck
            state.getDeck().add(aceOfHearts); // The bottom card of the deck is the trump

            p1.getHand().add(nineOfHearts);

            // 2. Stub the PUBLIC methods
            when(gameUtilService.getUsername()).thenReturn(p1Name);
            when(gameUtilService.findGameForClosingOrRemoval(p1Name)).thenReturn(game);

            // 3. Execute
            gameService.replaceCard();

            // 4. Assertions based on the logic in your GameService.replaceCard()
            // The trump card in state should now be the Nine
            assertThat(state.getTrumpCard().getRank()).isEqualTo(Rank.NINE);

            // The Nine should have been removed from hand and replaced by the Ace
            assertThat(p1.getHand()).contains(aceOfHearts);
            assertThat(p1.getHand()).doesNotContain(nineOfHearts);

            // Verify persistence
            verify(gameUtilService).saveGameState(state);
            verify(webSocketUtilService).updateGameState(game);
        }
    }

    @Nested
    @DisplayName("End Game Logic Tests")
    class EndGameTests {
        @Test
        void finishDeal_Success() {
            p1.setScore(66);
            p2.setIsBlanked(false);
            p2.setScore(20);

            when(gameUtilService.getUsername()).thenReturn(p1Name);
            when(gameUtilService.findGameByUsername(p1Name)).thenReturn(game);

            gameService.finishDeal();

            // P1 wins with 2 Result points because P2 is under 33 but not blanked
            assertThat(p1.getResult()).isEqualTo(2);
            verify(gameUtilService).prepareNewState(game, p1);
            verify(webSocketUtilService).updateGameState(any());
        }

        @Test
        void finishGame_Surrender_OpponentWins() {
            // 1. Setup: P1 is the one surrendering
            when(gameUtilService.getUsername()).thenReturn(p1Name);
            when(gameUtilService.findGameByUsername(p1Name)).thenReturn(game);
            // Note: We do NOT stub game.getOpponentPlayerByUsername()
            // because it's a real method on a real object.

            // 2. Execute
            gameService.finishGame();

            // 3. Assertions: Check if the real logic worked
            // The winner should be P2 because P1 surrendered
            assertThat(game.getWinner()).isEqualTo(p2);

            // Check if hands were cleared as per your service logic
            assertThat(p1.getHand()).isEmpty();
            assertThat(p2.getHand()).isEmpty();

            // Verify interactions
            verify(gameUtilService).saveGame(game);
            verify(webSocketUtilService).updateGameState(game);
        }
    }
}
