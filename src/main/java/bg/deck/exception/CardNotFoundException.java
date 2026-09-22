package bg.deck.exception;

import static bg.deck.constant.ExceptionConstants.CARD_NOT_FOUND;

public class CardNotFoundException extends RuntimeException {
    public CardNotFoundException(String username) {
        super(String.format(CARD_NOT_FOUND, username));
    }
}
