package bg.deck.santaseservice.model.request;

import bg.deck.santaseservice.constant.ValidationConstants;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
@NoArgsConstructor
public class RefreshRequest {
    @NotBlank(message = ValidationConstants.REFRESH_TOKEN_EMPTY)
    private String refreshToken;
}
