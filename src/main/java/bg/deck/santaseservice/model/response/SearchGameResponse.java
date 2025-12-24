package bg.deck.santaseservice.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@Getter
@Setter
@Builder
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchGameResponse {
    private final String status;
    private final UUID gameId;

    public static SearchGameResponse waiting() {
        return new SearchGameResponse("WAITING", null);
    }

    public static SearchGameResponse started(UUID gameId) {
        return new SearchGameResponse("GAME_STARTED", gameId);
    }
}
