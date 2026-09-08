package bg.deck.santaseservice.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfileResponse {

    /**
     * Kept, and still populated from the SANTASE stats row, purely so the
     * currently deployed frontend keeps working. New clients should read
     * {@link #stats} instead.
     */
    private int santaseWins;
    private int santaseLosses;
    private String rank;

    @JsonProperty("isEmailConfirmed")
    private boolean emailConfirmed;

    /** Per-game record, keyed by game type: SANTASE, TABLA. */
    private Map<String, GameStatsDTO> stats;
}
