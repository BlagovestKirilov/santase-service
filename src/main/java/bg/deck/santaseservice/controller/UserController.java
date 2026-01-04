package bg.deck.santaseservice.controller;

import bg.deck.santaseservice.model.response.ProfileResponse;
import bg.deck.santaseservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/user")
@RestController
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> getProfile() {
        return ResponseEntity.ok(userService.getProfile());
    }

    @PostMapping("/resend-email")
    public ResponseEntity<Void> confirmEmail() {
        if (userService.resendEmail()) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().build();
        }
    }
}
