package bg.deck.santaseservice.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ExceptionConstants {
    public static final String USERNAME_NOT_EXIST = "Username '%s' does not exist.";
    public static final String INVALID_TOKEN = "Invalid token.";
    public static final String USERNAME_ALREADY_EXISTS = "Username '%s' already exists.";
    public static final String INCORRECT_CREDENTIALS_MESSAGE = "Username or password is incorrect.";
    public static final String VALIDATION_ERROR_TITLE = "Validation Error.";
    public static final String INTERNAL_SERVER_ERROR_MESSAGE = "An unexpected error occurred.";

    public static final String LOG_FORMAT_SECURITY = "{} | IP: {} | Path: {}.";
    public static final String LOG_FORMAT_ERROR = "ERROR: {} | Path: {} | Details: {}.";
    public static final String LOG_FORMAT_UNHANDLED = "UNHANDLED EXCEPTION: IP: {} | Path: {} | {} at {}.";

    public static final String COMMA_DELIMITER = ", ";
    public static final String VALIDATION_DETAILS_FORMAT = "%s: %s";

    public static final String USER_NOT_PART_OF_GAME = "User: %s is not part of this game.";
    public static final String PLAYER_NOT_IN_TURN = "Player: %s is not in turn.";
    public static final String PLAYER_NOT_FIRST_IN_TURN = "Player: %s is not first in turn.";
    public static final String CARD_NOT_FOUND = "Card not found in player %s hand.";
    public static final String CARD_NOT_FOUND_FOR_REPLACING = "Card not found for replacing for player %s.";
    public static final String NO_ACTIVE_GAME = "No active game found for %s.";
    public static final String DECK_SIZE_EXCEPTION = "Deck size must be greater than %s and less than %s.";
    public static final String CARD_NOT_PLAYABLE = "Card not playable.";
}
