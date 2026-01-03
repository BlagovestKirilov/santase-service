package bg.deck.santaseservice.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfileResponse {
    private int santaseWins;
    private int santaseLosses;
}
