package bg.deck.santaseservice.constant;

public class ExceptionConstants {
    public static final String USERNAME_NOT_EXIST = "Username '%s' does not exist";
    public static final String INVALID_TOKEN = "Invalid token";
    public static final String USERNAME_ALREADY_EXISTS = "Username '%s' already exists";
    public static final String INCORRECT_CREDENTIALS_MESSAGE = "Username or password is incorrect";
    public static final String VALIDATION_ERROR_TITLE = "Validation Error";
    public static final String INTERNAL_SERVER_ERROR_MESSAGE = "An unexpected error occurred";

    public static final String LOG_FORMAT_SECURITY = "%s | IP: %s | Path: %s";
    public static final String LOG_FORMAT_ERROR = "ERROR: %s | Path: %s | Details: %s";
    public static final String LOG_FORMAT_UNHANDLED = "UNHANDLED EXCEPTION: IP: %s | Path: %s | %s at %s";

    public static final String COMMA_DELIMITER = ", ";
    public static final String VALIDATION_DETAILS_FORMAT = "%s: %s";
}
