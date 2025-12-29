package bg.deck.santaseservice.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class CardDTO {
    private UUID id;
    private String suit;
    private String rank;
    private int points;
    @JsonProperty("isPlayable")
    private boolean isPlayable;
    @JsonProperty("isLastDrawn")
    private boolean isLastDrawn;
}
