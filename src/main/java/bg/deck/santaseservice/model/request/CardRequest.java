package bg.deck.santaseservice.model.request;

import bg.deck.santaseservice.constant.ValidationConstants;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@ToString
@Getter
@Setter
@NoArgsConstructor
public class CardRequest {
    @NotNull(message = ValidationConstants.CARD_ID_NULL)
    private UUID cardId;
}
