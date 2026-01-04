package bg.deck.santaseservice.exception;

import static bg.deck.santaseservice.constant.ExceptionConstants.EMAIL_CONFIRMATION_NOT_FOUND;

public class EmailConfirmationNotFoundException extends RuntimeException {
    public EmailConfirmationNotFoundException(String confirmationToken) {
        super(String.format(EMAIL_CONFIRMATION_NOT_FOUND, confirmationToken));
    }
}
