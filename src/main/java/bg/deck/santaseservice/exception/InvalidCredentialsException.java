package bg.deck.santaseservice.exception;

import static bg.deck.santaseservice.constant.ExceptionConstants.INVALID_CREDENTIALS;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String username) {
        super(String.format(INVALID_CREDENTIALS, username));
    }
}
