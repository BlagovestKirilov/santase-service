package bg.deck.santaseservice.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * A табла position as one player sees it.
 *
 * <p>{@code legalHops} is the important field: the server enumerates every legal
 * move, so the client never re-implements the rules and the two can never
 * disagree about "use both dice", the higher-die rule or bearing off. It is
 * naturally empty for the player who is not on turn.
 */
@Getter
@Setter
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TablaStateResponse {

    private String gameId;
    private String gameType;

    private String firstPlayerUsername;
    private String secondPlayerUsername;

    /** WHITE or BLACK — which side this recipient plays. */
    private String mySide;

    /** 24 entries, canonical numbering; positive = WHITE checkers. */
    private List<Integer> points;

    private int myBar;
    private int opponentBar;
    private int myOff;
    private int opponentOff;

    private int myPipCount;
    private int opponentPipCount;

    @JsonProperty("isOnTurn")
    private boolean isOnTurn;

    private Integer die1;
    private Integer die2;
    private List<Integer> remainingDice;

    private int maxDiceUsable;
    private int usedDiceCount;
    private boolean mustConfirm;
    private boolean noMovesAvailable;

    private List<HopDTO> legalHops;
    private List<HopDTO> pendingHops;

    private String winnerUsername;
    private String surrenderPlayerUsername;
    /** SINGLE, GAMMON (марс) or BACKGAMMON (кокс). Display only. */
    private String resultKind;

    private int inactivityCount;
    private Integer nextMoveTimeInSeconds;

    /** Published from move one so the dice can be verified afterwards. */
    private String serverSeedHash;
    /** Revealed only once the game is finished. */
    private String serverSeed;
}
