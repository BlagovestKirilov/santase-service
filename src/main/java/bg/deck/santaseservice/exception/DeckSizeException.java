package bg.deck.santaseservice.exception;

import static bg.deck.santaseservice.constant.ExceptionConstants.DECK_SIZE_EXCEPTION;

public class DeckSizeException extends RuntimeException {
    public DeckSizeException(int min, int max) {
        super(String.format(DECK_SIZE_EXCEPTION, min, max));
    }
}
