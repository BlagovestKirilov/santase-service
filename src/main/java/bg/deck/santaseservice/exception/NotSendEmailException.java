package bg.deck.santaseservice.exception;

import static bg.deck.santaseservice.constant.ExceptionConstants.FAILED_SENDING_EMAIL;

public class NotSendEmailException extends RuntimeException {
    public NotSendEmailException(String email) {
        super(String.format(FAILED_SENDING_EMAIL, email));
    }
}
