package bg.deck.exception;

import bg.deck.constant.ExceptionConstants;

public class PlayerInactivitySurrenderException extends RuntimeException {
    public PlayerInactivitySurrenderException() {
        super(ExceptionConstants.PLAYER_SURRENDERED_DUE_TO_INACTIVITY);
    }
}
