package bg.deck.santaseservice.exception;

import static bg.deck.santaseservice.constant.ExceptionConstants.PLAYER_NOT_IN_TURN;

public class NotInTurnException extends RuntimeException {
    public NotInTurnException(String username) {
        super(String.format(PLAYER_NOT_IN_TURN, username));
    }
}
