package bg.deck.santaseservice.exception;

import static bg.deck.santaseservice.constant.ExceptionConstants.USER_NOT_FOUND;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String username) {
        super(String.format(USER_NOT_FOUND, username));
    }
}
