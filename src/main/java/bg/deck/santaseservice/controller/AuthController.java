package bg.deck.santaseservice.controller;

import bg.deck.santaseservice.model.request.ChangePasswordRequest;
import bg.deck.santaseservice.model.request.ForgotPasswordRequest;
import bg.deck.santaseservice.model.request.LoginRequest;
import bg.deck.santaseservice.model.request.RefreshRequest;
import bg.deck.santaseservice.model.request.RegisterRequest;
import bg.deck.santaseservice.model.response.AuthResponse;
import bg.deck.santaseservice.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

import static bg.deck.santaseservice.constant.Constants.DECK_BG_CONFIRMATION_INVALID;
import static bg.deck.santaseservice.constant.Constants.DECK_BG_SUCCESS_CONFIRMATION;

@RequiredArgsConstructor
@RequestMapping("/auth")
@RestController
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        return ResponseEntity.ok(authService.login(loginRequest, request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        return ResponseEntity.ok(authService.register(registerRequest));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken()));
    }

    @GetMapping("/confirm-email")
    public ResponseEntity<Void> confirmEmail(@RequestParam("token") String confirmationToken) {

        boolean confirmed = authService.confirmEmail(confirmationToken);

        String redirectUrl = confirmed ? DECK_BG_SUCCESS_CONFIRMATION : DECK_BG_CONFIRMATION_INVALID;

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
    }

    @GetMapping("/validate-link")
    public ResponseEntity<Void> validateForgotPassword(@RequestParam("token") String forgotPasswordToken) {
        authService.validateForgotPassword(forgotPasswordToken);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest) {
        authService.forgotPassword(forgotPasswordRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@RequestBody ChangePasswordRequest changePasswordRequest) {
        authService.changePassword(changePasswordRequest);
        return ResponseEntity.ok().build();
    }
}
