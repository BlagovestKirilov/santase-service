package bg.deck.santaseservice.exception;

import static bg.deck.santaseservice.constant.ExceptionConstants.USERNAME_NOT_EXIST;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String username) {
        super(String.format(USERNAME_NOT_EXIST, username));
    }
}
