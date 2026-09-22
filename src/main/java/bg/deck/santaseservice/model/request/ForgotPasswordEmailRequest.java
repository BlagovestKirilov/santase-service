package bg.deck.santaseservice.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordEmailRequest(
        @NotBlank
        @Email
        String email
) {
}
