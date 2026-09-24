package bg.deck.belot;

import bg.deck.belot.engine.DealOutcome;
import bg.deck.belot.engine.DealResult;
import bg.deck.belot.engine.DealScorer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * RULES §8 — settling a deal: made, вътре, висящи.
 */
@DisplayName("Settling a deal")
class BelotDealScoringTest {

    /** A plain deal, nothing doubled, nothing hanging from before. */
    private static DealOutcome plain(int callerPoints, int opponentPoints) {
        return DealScorer.score(callerPoints, opponentPoints, 1, 0);
    }

    @Nested
    @DisplayName("made")
    class Made {

        @Test
        @DisplayName("the callers took more, so each side records its own")
        void eachRecordsItsOwn() {
            DealOutcome outcome = plain(90, 72);

            assertEquals(DealResult.MADE, outcome.result());
            assertEquals(9, outcome.recorded().caller());
            assertEquals(7, outcome.recorded().opponents());
            assertEquals(0, outcome.hanging());
        }

        @Test
        @DisplayName("one point in it is enough")
        void aSinglePointIsEnough() {
            assertEquals(DealResult.MADE, plain(82, 80).result());
        }
    }

    @Nested
    @DisplayName("вътре")
    class Inside {

        @Test
        @DisplayName("the others took more, so they record everything and the callers nothing")
        void theOthersTakeTheLot() {
            DealOutcome outcome = plain(80, 82);

            assertEquals(DealResult.INSIDE, outcome.result());
            assertEquals(0, outcome.recorded().caller());
            assertEquals(16, outcome.recorded().opponents(), "162 in the deal, all of it theirs");
            assertEquals(0, outcome.hanging());
        }

        @Test
        @DisplayName("bonuses go the same way — the whole deal is theirs")
        void bonusesGoTooLegacy() {
            // The callers held a quarte, and still went down: 50 of those 130
            // points are theirs, and they lose them with the rest.
            DealOutcome outcome = plain(130, 132);

            assertEquals(26, outcome.recorded().opponents(), "262 between them, recorded as 26");
            assertEquals(0, outcome.recorded().caller());
        }
    }

    @Nested
    @DisplayName("висящи")
    class Hanging {

        @Test
        @DisplayName("level: the callers record nothing and their points wait")
        void levelLeavesThePointsOnTheTable() {
            DealOutcome outcome = plain(81, 81);

            assertEquals(DealResult.HANGING, outcome.result());
            assertEquals(0, outcome.recorded().caller(), "«не записва точките си»");
            assertEquals(8, outcome.recorded().opponents(), "the others record theirs as usual");
            assertEquals(8, outcome.hanging(), "and the callers' eight waits for the next deal");
        }

        @Test
        @DisplayName("what hangs is picked up by whoever makes the next deal")
        void theNextWinnerCollectsIt() {
            DealOutcome hung = plain(81, 81);
            DealOutcome next = DealScorer.score(90, 72, 1, hung.hanging());

            assertEquals(17, next.recorded().caller(), "nine of their own and the eight that was waiting");
            assertEquals(7, next.recorded().opponents());
            assertEquals(0, next.hanging(), "nothing is left over");
        }

        @Test
        @DisplayName("and it keeps waiting through another level deal")
        void hangingOnHanging() {
            DealOutcome first = plain(81, 81);
            DealOutcome second = DealScorer.score(81, 81, 1, first.hanging());

            assertEquals(16, second.hanging(), "eight from each deal, still on the table");
        }
    }

    @Nested
    @DisplayName("contra")
    class Doubled {

        @Test
        @DisplayName("doubles what each side records")
        void doubled() {
            DealOutcome outcome = DealScorer.score(90, 72, 2, 0);

            assertEquals(18, outcome.recorded().caller());
            assertEquals(14, outcome.recorded().opponents());
        }

        @Test
        @DisplayName("quadruples it after a recontra")
        void quadrupled() {
            DealOutcome outcome = DealScorer.score(90, 72, 4, 0);

            assertEquals(36, outcome.recorded().caller());
            assertEquals(28, outcome.recorded().opponents());
        }

        @Test
        @DisplayName("and a deal that goes down doubles for the others")
        void insideDoubled() {
            DealOutcome outcome = DealScorer.score(80, 82, 2, 0);

            assertEquals(32, outcome.recorded().opponents(), "16 for the deal, doubled");
            assertEquals(0, outcome.recorded().caller());
        }

        @Test
        @DisplayName("points that hang while doubled carry forward doubled")
        void hangingStaysDoubled() {
            DealOutcome outcome = DealScorer.score(81, 81, 2, 0);

            assertEquals(16, outcome.hanging(), "«всички точки (удвоени или учетворени) остават»");
        }
    }

    @Nested
    @DisplayName("still to be settled")
    class Open {

        @Test
        @Disabled("""
                OPEN 16 — a contra doubles the recorded score here, after rounding. \
                The alternative is doubling the raw points and rounding that, which \
                can differ by a point. Which is it?""")
        @DisplayName("whether the doubling comes before or after the rounding")
        void whenTheDoublingHappens() {
        }

        @Test
        @Disabled("""
                OPEN 17 — hanging points are handed to the team that wins the next \
                deal. When that next deal goes вътре, the winners are the defenders \
                — do they collect what was hanging? This assumes yes: whoever \
                records the deal takes it.""")
        @DisplayName("who collects the hanging points when the next deal goes down")
        void whoCollectsAfterAFailedDeal() {
        }
    }
}
