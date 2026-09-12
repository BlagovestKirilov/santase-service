package bg.deck.santaseservice.model.request;

import bg.deck.santaseservice.constant.ValidationConstants;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * The token from the account-deletion email, sent in the body rather than the
 * query string so it does not end up in access logs or a {@code Referer}.
 */
@Getter
@Setter
@NoArgsConstructor
public class ConfirmDeletionRequest {
    @NotNull(message = ValidationConstants.TOKEN_NULL)
    private UUID token;
}
