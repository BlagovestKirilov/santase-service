package bg.deck.exception;

import bg.deck.constant.ExceptionConstants;

public class EmailNotConfirmedException extends RuntimeException {
    public EmailNotConfirmedException(String email) {
        super(String.format(ExceptionConstants.EMAIL_NOT_CONFIRMED, email));
    }
}