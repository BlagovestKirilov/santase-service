package bg.deck.santaseservice.model.request;

import bg.deck.santaseservice.constant.ValidationConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
@NoArgsConstructor
public class RegisterRequest {
    @NotBlank(message = ValidationConstants.USERNAME_EMPTY)
    @Size(min = ValidationConstants.USERNAME_MIN, max = ValidationConstants.USERNAME_MAX, message = ValidationConstants.USERNAME_SIZE)
    @Pattern(regexp = ValidationConstants.ALPHANUMERIC_PATTERN, message = ValidationConstants.USERNAME_PATTERN)
    private String username;

    @NotBlank(message = ValidationConstants.PASSWORD_EMPTY)
    @Size(min = ValidationConstants.PASSWORD_MIN, max = ValidationConstants.PASSWORD_MAX, message = ValidationConstants.PASSWORD_SIZE)
    @Pattern(regexp = ValidationConstants.PASSWORD_PATTERN, message = ValidationConstants.PASSWORD_PATTERN_MSG)
    private String password;
}
