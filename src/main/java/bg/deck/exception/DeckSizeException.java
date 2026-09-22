package bg.deck.exception;

import static bg.deck.constant.ExceptionConstants.DECK_SIZE_EXCEPTION;

public class DeckSizeException extends RuntimeException {
    public DeckSizeException(int min, int max) {
        super(String.format(DECK_SIZE_EXCEPTION, min, max));
    }
}
