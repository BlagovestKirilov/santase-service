package bg.deck.santaseservice.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class LogConstants {
    public static final String SUCCESSFUL_LOGIN_LOG = "Successfully logged in user: {}";
    public static final String TRY_LOGIN_LOG = "Trying to login account with username {}";
    public static final String SUCCESSFUL_REGISTER_LOG = "Successfully registered user: {}";
    public static final String TRY_REGISTER_LOG = "Trying to register account with username {}";
    public static final String TRY_REFRESH_TOKEN = "Trying to refresh token user: {}";
    public static final String TRY_GET_PROFILE = "Trying to get profile user: {}";
    public static final String GET_STATE_LOG = "Trying to get game state: {}";
    public static final String GOT_STATE_LOG = "Successfully got game state: {}";
    public static final String GAME_SEARCH_START = "Initiating game search for user: {}";
    public static final String GAME_SEARCH_ALREADY_IN_QUEUE = "User {} is already in the match queue. Notifying client.";
    public static final String GAME_SEARCH_ADDED_TO_QUEUE = "No waiting players found. Added user {} to match queue.";
    public static final String GAME_SEARCH_MATCH_FOUND = "Match found! Pairing {} with {} in game session: {}";
    public static final String GAME_SEARCH_USER_UNAVAILABLE = "Game search aborted. User {} does not exist or is currently unavailable.";
    public static final String PLAY_CARD_START = "User {} is attempting to play card: {}";
    public static final String PLAY_CARD_TRICK_EVALUATING = "Both players played. Evaluating trick for cards: {}, {}";
    public static final String PLAY_CARD_SUCCESS = "User {} successfully played {}. Turn moving to opponent.";
    public static final String ANNOUNCE_START = "User {} is attempting to announce a combination with card: {}";
    public static final String ANNOUNCE_SUCCESS = "User {} successfully announced a combination. Bonus: {}";
    public static final String CLOSE_DECK_START = "User {} is attempting to close the deck.";
    public static final String CLOSE_DECK_SUCCESS = "Deck closed by user: {} Remaining cards removed from play.";
    public static final String REPLACE_CARD_START = "User {} is attempting to replace the trump card with the Nine of {}";
    public static final String REPLACE_CARD_SUCCESS = "User {} successfully swapped the Nine for the Trump card ({}).";
    public static final String FINISH_DEAL_START = "User {} is attempting to claim 66 points and finish the deal.";
    public static final String FINISH_DEAL_SUCCESS = "Deal finished. Winner: {} | Points awarded: {}.";
    public static final String FINISH_GAME_SURRENDER = "User {} has surrendered. Opponent {} wins the game.";
    public static final String FINISH_GAME = "Game finished gameId={} winner={} result={}({}) vs {}({}).";
}
