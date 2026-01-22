package bg.deck.santaseservice.model.response;

import bg.deck.santaseservice.enums.SearchGameStatus;
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
        return new SearchGameResponse(SearchGameStatus.WAITING.toString(), null);
    }

    public static SearchGameResponse started(UUID gameId) {
        return new SearchGameResponse(SearchGameStatus.GAME_STARTED.toString(), gameId);
    }
}
