package bg.deck.santaseservice.exception;

import bg.deck.santaseservice.constant.ExceptionConstants;

public class PlayerInactivitySurrenderException extends RuntimeException {
    public PlayerInactivitySurrenderException() {
        super(ExceptionConstants.PLAYER_SURRENDERED_DUE_TO_INACTIVITY);
    }
}
