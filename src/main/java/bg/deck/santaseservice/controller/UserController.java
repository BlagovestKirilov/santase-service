package bg.deck.santaseservice.controller;

import bg.deck.santaseservice.annotation.ValidUUID;
import bg.deck.santaseservice.constant.Constants;
import bg.deck.santaseservice.model.request.ChangePasswordRequest;
import bg.deck.santaseservice.model.request.UserDeletionRequest;
import bg.deck.santaseservice.model.response.ProfileResponse;
import bg.deck.santaseservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Validated
@RequiredArgsConstructor
@RequestMapping("/user")
@RestController
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> getProfile() {
        return ResponseEntity.ok(userService.getProfile());
    }

    @PostMapping("/confirm-email")
    public ResponseEntity<Void> confirmEmail() {
        return userService.confirmEmail() ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest changePasswordRequest) {
        userService.changePassword(changePasswordRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/delete-user")
    public ResponseEntity<Void> sendUserDeletionEmail(@Valid @RequestBody UserDeletionRequest userDeletionRequest) {
        userService.sendUserDeletionEmail(userDeletionRequest);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/confirm-deletion")
    public ResponseEntity<Void> verifyForgotPasswordToken(@ValidUUID @RequestParam("token") String userDeletionToken) {
        boolean isDeleted = userService.confirmDeletion(userDeletionToken);

        String redirectUrl = isDeleted ? Constants.DECK_BG_SUCCESS_DELETION : Constants.DECK_BG_INVALID_LINK;

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
    }
}
