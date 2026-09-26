package bg.deck.exception;

/**
 * Thrown when a player asks to start a game that is not being offered to them
 * — switched off, or kept for a few testers while it is still being built.
 *
 * <p>Answered with 404. A game somebody may not have should not announce that
 * it exists: a beta before its time is not a locked door with a label on it, it
 * is simply not there.
 */
public class ServiceNotAvailableException extends RuntimeException {

    public ServiceNotAvailableException(String code) {
        super(code + " is not on offer.");
    }
}
