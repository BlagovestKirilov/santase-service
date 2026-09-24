package bg.deck.belot.controller;

import bg.deck.belot.service.BelotPlayerService;
import bg.deck.util.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/belot")
public class BelotController {

    private final BelotPlayerService belotPlayerService;

    /**
     * Proves the seam end to end: a real session reaches belot, and belot
     * writes to its own schema and nowhere else.
     */
    @GetMapping("/ping")
    public ResponseEntity<Void> ping() {
        belotPlayerService.ensureKnown(AuthenticatedUser.username());
        return ResponseEntity.noContent().build();
    }
}
