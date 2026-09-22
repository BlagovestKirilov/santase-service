package bg.deck.santaseservice.model.request;

import bg.deck.santaseservice.constant.ValidationConstants;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CardRequest(
        @NotNull(message = ValidationConstants.CARD_ID_NULL)
        UUID cardId
) {
}
