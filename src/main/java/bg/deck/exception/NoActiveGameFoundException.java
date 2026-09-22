package bg.deck.exception;

import static bg.deck.constant.ExceptionConstants.NO_ACTIVE_GAME;

public class NoActiveGameFoundException extends RuntimeException {
    public NoActiveGameFoundException(String username) {
        super(String.format(NO_ACTIVE_GAME, username));
    }
}
