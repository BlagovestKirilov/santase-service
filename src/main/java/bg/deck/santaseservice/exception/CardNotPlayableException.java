package bg.deck.santaseservice.exception;

import static bg.deck.santaseservice.constant.ExceptionConstants.CARD_NOT_PLAYABLE;

public class CardNotPlayableException extends RuntimeException {
    public CardNotPlayableException() {
        super(CARD_NOT_PLAYABLE);
    }
}
