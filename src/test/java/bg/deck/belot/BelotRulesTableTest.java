package bg.deck.belot;

import bg.deck.belot.engine.Card;
import bg.deck.belot.engine.CardPoints;
import bg.deck.belot.engine.Contract;
import bg.deck.belot.engine.Deck;
import bg.deck.belot.engine.Rank;
import bg.deck.belot.engine.Suit;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rules of belot, one test per rule, read straight off
 * {@code docs/belot/RULES.md}.
 *
 * <p>This is the table the engine is built against: a rule that is not here is
 * a rule nobody has agreed on yet. Where the rules page is silent the test is
 * {@code @Disabled} and names the open question — a disabled test is a question
 * waiting for an answer, and it keeps that question in front of whoever runs
 * the suite instead of in a document nobody opens.
 */
@DisplayName("The rules of belot")
class BelotRulesTableTest {

    @Nested
    @DisplayName("§2 — which card beats which")
    class CardOrder {

        @Test
        @DisplayName("in a trump suit: J 9 A 10 K Q 8 7")
        void trumpOrder() {
            assertEquals(
                    List.of(Rank.JACK, Rank.NINE, Rank.ACE, Rank.TEN, Rank.KING, Rank.QUEEN, Rank.EIGHT, Rank.SEVEN),
                    ranksByStrength(true));
        }

        @Test
        @DisplayName("in a plain suit: A 10 K Q J 9 8 7")
        void plainOrder() {
            assertEquals(
                    List.of(Rank.ACE, Rank.TEN, Rank.KING, Rank.QUEEN, Rank.JACK, Rank.NINE, Rank.EIGHT, Rank.SEVEN),
                    ranksByStrength(false));
        }

        @Test
        @DisplayName("the jack and the nine are only mighty in trumps")
        void theJackIsOnlyMightyInTrumps() {
            assertTrue(Rank.JACK.strength(true) > Rank.ACE.strength(true));
            assertTrue(Rank.JACK.strength(false) < Rank.ACE.strength(false));
            assertTrue(Rank.NINE.strength(true) > Rank.ACE.strength(true));
            assertTrue(Rank.NINE.strength(false) < Rank.JACK.strength(false));
        }

        private static List<Rank> ranksByStrength(boolean trump) {
            return java.util.Arrays.stream(Rank.values())
                    .sorted(Comparator.comparingInt((Rank rank) -> rank.strength(trump)).reversed())
                    .toList();
        }
    }

    @Nested
    @DisplayName("§3 — what cards are worth")
    class Points {

        @ParameterizedTest(name = "{0} in trumps is {1}, in a plain suit {2}")
        @CsvSource({
                "JACK,  20, 2",
                "NINE,  14, 0",
                "ACE,   11, 11",
                "TEN,   10, 10",
                "KING,   4, 4",
                "QUEEN,  3, 3",
                "EIGHT,  0, 0",
                "SEVEN,  0, 0",
        })
        void pointsPerRank(Rank rank, int inTrumps, int inPlain) {
            assertEquals(inTrumps, rank.points(true));
            assertEquals(inPlain, rank.points(false));
        }

        @Test
        @DisplayName("a suit deal is worth 162")
        void suitDealTotal() {
            assertEquals(162, CardPoints.dealTotal(Contract.SPADES));
        }

        @Test
        @DisplayName("every suit contract is worth the same")
        void everySuitIsTheSame() {
            for (Contract contract : List.of(Contract.CLUBS, Contract.DIAMONDS, Contract.HEARTS, Contract.SPADES)) {
                assertEquals(162, CardPoints.dealTotal(contract), contract.name());
            }
        }

        @Test
        @DisplayName("all trumps is worth 258")
        void allTrumpsDealTotal() {
            assertEquals(258, CardPoints.dealTotal(Contract.ALL_TRUMPS));
        }

        @Test
        @Disabled("""
                OPEN 1 — the rules page says a no-trump deal is worth 260, but the \
                cards add to 130. That works only if every point in a no-trump deal \
                is doubled, the last trick included. Confirm, then delete this \
                @Disabled and fix CardPoints.""")
        @DisplayName("no trumps is worth 260 — doubled?")
        void noTrumpsDealTotal() {
            assertEquals(260, CardPoints.dealTotal(Contract.NO_TRUMPS));
        }

        @Test
        @DisplayName("the undoubled no-trump arithmetic, so the gap is visible")
        void noTrumpsUndoubledIsHalfOfIt() {
            assertEquals(130, CardPoints.dealTotal(Contract.NO_TRUMPS),
                    "130 is exactly half of the 260 the rules claim — see OPEN 1");
        }
    }

    @Nested
    @DisplayName("§5 — which contract outbids which")
    class Bidding {

        @Test
        @DisplayName("clubs < diamonds < hearts < spades < no trumps < all trumps")
        void biddingOrder() {
            assertEquals(
                    List.of(Contract.CLUBS, Contract.DIAMONDS, Contract.HEARTS,
                            Contract.SPADES, Contract.NO_TRUMPS, Contract.ALL_TRUMPS),
                    List.of(Contract.values()),
                    "OPEN 2 — the rules page prints its suit list with spades twice; this is the usual order");
        }

        @Test
        @DisplayName("a contract beats everything below it and nothing above")
        void beatsIsStrictlyAscending() {
            Contract[] all = Contract.values();
            for (int higher = 0; higher < all.length; higher++) {
                for (int lower = 0; lower < all.length; lower++) {
                    assertEquals(higher > lower, all[higher].beats(all[lower]),
                            all[higher] + " vs " + all[lower]);
                }
            }
        }
    }

    @Nested
    @DisplayName("which suits are trumps")
    class Trumps {

        @ParameterizedTest
        @EnumSource(Suit.class)
        @DisplayName("all trumps: every suit")
        void allTrumps(Suit suit) {
            assertTrue(Contract.ALL_TRUMPS.isTrump(suit));
        }

        @ParameterizedTest
        @EnumSource(Suit.class)
        @DisplayName("no trumps: none of them")
        void noTrumps(Suit suit) {
            assertFalse(Contract.NO_TRUMPS.isTrump(suit));
        }

        @Test
        @DisplayName("a suit contract: that one and no other")
        void oneSuit() {
            assertTrue(Contract.HEARTS.isTrump(Suit.HEARTS));
            assertFalse(Contract.HEARTS.isTrump(Suit.SPADES));
            assertFalse(Contract.HEARTS.isTrump(Suit.CLUBS));
            assertFalse(Contract.HEARTS.isTrump(Suit.DIAMONDS));
        }
    }

    @Nested
    @DisplayName("§7 — the order sequences are built from")
    class NaturalOrder {

        @Test
        @DisplayName("7 8 9 10 J Q K A, whatever the contract")
        void naturalOrderIsTheDeclarationOrder() {
            assertEquals(
                    List.of(Rank.SEVEN, Rank.EIGHT, Rank.NINE, Rank.TEN,
                            Rank.JACK, Rank.QUEEN, Rank.KING, Rank.ACE),
                    java.util.Arrays.stream(Rank.values())
                            .sorted(java.util.Comparator.comparingInt(Rank::naturalOrder))
                            .toList(),
                    "Declarations reads runs off this order; the enum is declared in it");
        }
    }

    @Nested
    @DisplayName("the deck")
    class TheDeck {

        @Test
        @DisplayName("32 cards, each one once")
        void thirtyTwoDistinctCards() {
            List<Card> deck = Deck.full();

            assertEquals(32, deck.size());
            assertEquals(32, Set.copyOf(deck).size(), "no card appears twice");
        }
    }

    /* ---------------- the rules nobody has answered yet ---------------- */

    @Nested
    @DisplayName("waiting on an answer")
    class Open {

        @Test
        @Disabled("OPEN 4 — in all trumps, with a partner winning the trick, must you still beat the led suit?")
        void allTrumpsObligationWhenPartnerIsWinning() {
        }

        @Test
        @Disabled("OPEN 5 — do fours compete with sequences, or is each compared on its own?")
        void foursVersusSequences() {
        }

        @Test
        @Disabled("OPEN 6 — does belote (K+Q of trumps) score even when the other team holds the best sequence?")
        void beloteIsIndependent() {
        }

        @Test
        @Disabled("OPEN 7 — which four wins when both teams hold one: J > 9 > A > 10 > K > Q?")
        void whichFourWins() {
        }

        @Test
        @Disabled("OPEN 9 — rounding: is 154 recorded as 15 or 16, and is the rule the same for both teams?")
        void rounding() {
        }

        @Test
        @Disabled("OPEN 10 — both teams cross 151 in the same deal: who wins, and what if the totals are equal?")
        void bothTeamsCrossTheLine() {
        }
    }
}
