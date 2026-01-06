package bg.deck.santaseservice.exception;

import bg.deck.santaseservice.constant.ExceptionConstants;

public class EmailNotConfirmedException extends RuntimeException {
    public EmailNotConfirmedException(String email) {
        super(String.format(ExceptionConstants.EMAIL_NOT_CONFIRMED, email));
    }
}