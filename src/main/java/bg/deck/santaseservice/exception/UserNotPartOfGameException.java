package bg.deck.santaseservice.exception;

import static bg.deck.santaseservice.constant.ExceptionConstants.USER_NOT_PART_OF_GAME;

public class UserNotPartOfGameException extends RuntimeException {
    public UserNotPartOfGameException(String username) {
        super(String.format(USER_NOT_PART_OF_GAME, username));
    }
}
