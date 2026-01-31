package bg.deck.santaseservice.model.request;

import bg.deck.santaseservice.constant.ValidationConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ChangeForgottenPasswordRequest {
    @NotBlank(message = ValidationConstants.PASSWORD_EMPTY)
    @Size(min = ValidationConstants.PASSWORD_MIN, max = ValidationConstants.PASSWORD_MAX, message = ValidationConstants.PASSWORD_SIZE)
    @Pattern(regexp = ValidationConstants.PASSWORD_PATTERN, message = ValidationConstants.PASSWORD_PATTERN_MSG)
    private String newPassword;

    @NotNull(message = ValidationConstants.TOKEN_NULL)
    private UUID token;
}
