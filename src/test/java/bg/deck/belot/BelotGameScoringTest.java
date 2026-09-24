package bg.deck.belot;

import bg.deck.belot.engine.GameScorer;
import bg.deck.belot.engine.GameVerdict;
import bg.deck.belot.engine.Seat;
import bg.deck.belot.engine.Team;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RULES §9 — when a game is over.
 */
@DisplayName("Winning a game")
class BelotGameScoringTest {

    private static GameVerdict after(int northSouth, int eastWest) {
        return GameScorer.verdict(northSouth, eastWest, false);
    }

    private static GameVerdict afterCapot(int northSouth, int eastWest) {
        return GameScorer.verdict(northSouth, eastWest, true);
    }

    @Nested
    @DisplayName("the line at 151")
    class TheLine {

        @Test
        @DisplayName("below it, the game goes on")
        void belowTheLine() {
            assertFalse(after(150, 96).isFinished());
        }

        @Test
        @DisplayName("cross it and the game is won")
        void crossingIt() {
            GameVerdict verdict = after(151, 96);

            assertTrue(verdict.isFinished());
            assertEquals(Team.NORTH_SOUTH, verdict.winningTeam().orElseThrow());
        }

        @Test
        @DisplayName("both across, and the higher total takes it")
        void bothAcross() {
            assertEquals(Team.EAST_WEST, after(155, 160).winningTeam().orElseThrow());
            assertEquals(Team.NORTH_SOUTH, after(206, 151).winningTeam().orElseThrow());
        }
    }

    @Nested
    @DisplayName("«с капо не се излиза»")
    class NoCapotFinish {

        @Test
        @DisplayName("a capot cannot be the deal that ends it")
        void aCapotDoesNotFinishIt() {
            assertFalse(afterCapot(151, 96).isFinished(), "one more deal follows");
        }

        @Test
        @DisplayName("and the deal after it can")
        void theNextDealCan() {
            GameVerdict verdict = after(168, 96);

            assertTrue(verdict.isFinished());
            assertEquals(Team.NORTH_SOUTH, verdict.winningTeam().orElseThrow());
        }

        @Test
        @DisplayName("a capot well short of the line changes nothing")
        void aCapotBelowTheLine() {
            assertFalse(afterCapot(120, 96).isFinished(), "nobody was finishing anyway");
        }
    }

    @Nested
    @DisplayName("teams")
    class Teams {

        @Test
        @DisplayName("partners share one")
        void partnersShareATeam() {
            assertEquals(Team.NORTH_SOUTH, Team.of(Seat.NORTH));
            assertEquals(Team.NORTH_SOUTH, Team.of(Seat.SOUTH));
            assertEquals(Team.EAST_WEST, Team.of(Seat.EAST));
            assertEquals(Team.EAST_WEST, Team.of(Seat.WEST));
            assertEquals(Team.EAST_WEST, Team.NORTH_SOUTH.opponent());
        }
    }

    @Nested
    @DisplayName("still to be settled")
    class Open {

        @Test
        @DisplayName("OPEN 10 — level on the line, another deal is played")
        void openTenALevelFinish() {
            assertFalse(after(151, 151).isFinished(),
                    "assumed: a game is not left drawn, so another deal follows. "
                            + "If the callers win a tie, or the game ends drawn, this changes.");
        }

        @Test
        @Disabled("""
                OPEN 11 — the extra deal after a capot. If that deal is itself a \
                capot, or everyone passes it, does yet another follow? The rules \
                page excludes all-pass rounds and earlier capot deals from the \
                count without saying what that means in play.""")
        @DisplayName("what the extra deal may itself be")
        void openElevenTheExtraDeal() {
        }
    }
}
