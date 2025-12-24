package bg.deck.santaseservice.exception;

import static bg.deck.santaseservice.constant.ExceptionConstants.INVALID_TOKEN;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException() {
        super(INVALID_TOKEN);
    }
}
