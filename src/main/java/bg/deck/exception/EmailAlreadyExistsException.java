package bg.deck.exception;

import static bg.deck.constant.ExceptionConstants.EMAIL_ALREADY_EXISTS;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email) {
        super(String.format(EMAIL_ALREADY_EXISTS, email));
    }
}
