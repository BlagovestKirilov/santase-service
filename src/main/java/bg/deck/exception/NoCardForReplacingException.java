package bg.deck.exception;

import static bg.deck.constant.ExceptionConstants.CARD_NOT_FOUND_FOR_REPLACING;

public class NoCardForReplacingException extends RuntimeException {
    public NoCardForReplacingException(String username) {
        super(String.format(CARD_NOT_FOUND_FOR_REPLACING, username));
    }
}
