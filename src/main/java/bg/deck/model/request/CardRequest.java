package bg.deck.model.request;

import bg.deck.constant.ValidationConstants;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CardRequest(
        @NotNull(message = ValidationConstants.CARD_ID_NULL)
        UUID cardId
) {
}
