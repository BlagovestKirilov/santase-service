package bg.deck.exception;

import static bg.deck.constant.ExceptionConstants.INVALID_TOKEN;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException() {
        super(INVALID_TOKEN);
    }
}
