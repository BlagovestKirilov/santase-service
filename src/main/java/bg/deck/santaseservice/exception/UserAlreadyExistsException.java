package bg.deck.santaseservice.exception;

import static bg.deck.santaseservice.constant.ExceptionConstants.USERNAME_ALREADY_EXISTS;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String username) {
        super(String.format(USERNAME_ALREADY_EXISTS, username));
    }
}
