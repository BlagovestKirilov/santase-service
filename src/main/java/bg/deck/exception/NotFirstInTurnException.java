package bg.deck.exception;

import static bg.deck.constant.ExceptionConstants.PLAYER_NOT_FIRST_IN_TURN;

public class NotFirstInTurnException extends RuntimeException {
    public NotFirstInTurnException(String username) {
        super(String.format(PLAYER_NOT_FIRST_IN_TURN, username));
    }
}
