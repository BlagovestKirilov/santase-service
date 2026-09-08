package bg.deck.santaseservice.model;

import java.time.Instant;

/**
 * The part of a game's state the turn timer cares about.
 *
 * <p>Implemented by both {@link GameState} (Santase) and {@link TablaGameState},
 * so {@link bg.deck.santaseservice.service.GameInactivityService} and the
 * inactivity-surrender path work for both games without branching on game type.
 */
public interface TurnClock {

    /** Seconds a player has to act before the inactivity timer fires. */
    int TURN_SECONDS = 33;

    Player getInTurnPlayer();

    /** Hands the turn to {@code player} and restarts the clock. */
    void setInTurnPlayer(Player player);

    Instant getNextMoveTime();

    /** Pushes the deadline out by a fresh {@link #TURN_SECONDS}. */
    void extendNextMoveTime();

    boolean isInTurn(Player player);
}
