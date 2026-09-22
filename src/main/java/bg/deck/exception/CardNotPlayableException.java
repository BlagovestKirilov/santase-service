package bg.deck.exception;

import static bg.deck.constant.ExceptionConstants.CARD_NOT_PLAYABLE;

public class CardNotPlayableException extends RuntimeException {
    public CardNotPlayableException() {
        super(CARD_NOT_PLAYABLE);
    }
}
