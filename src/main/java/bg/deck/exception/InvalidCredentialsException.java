package bg.deck.exception;

import static bg.deck.constant.ExceptionConstants.INVALID_CREDENTIALS;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String username) {
        super(String.format(INVALID_CREDENTIALS, username));
    }
}
