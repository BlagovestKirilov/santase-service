package bg.deck.controller;

import bg.deck.model.request.CardRequest;
import bg.deck.service.GameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import bg.deck.config.RequiresService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/game")
@RestController
public class GameController {

    private final GameService gameService;

    // Only the start of a game is gated: switching сантасе off stops new
    // games and lets the ones being played finish.
    @RequiresService("SANTASE")
    @PostMapping("/search")
    public ResponseEntity<Void> searchGame() {
        gameService.searchGame();
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/state")
    public ResponseEntity<Void> state() {
        gameService.getGameState();
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/play-card")
    public ResponseEntity<Void> playCard(@Valid @RequestBody CardRequest cardRequest) {
        gameService.playCard(cardRequest);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/announce")
    public ResponseEntity<Void> announceCombination(@Valid @RequestBody CardRequest cardRequest) {
        gameService.announceCombination(cardRequest);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/close-deck")
    public ResponseEntity<Void> closeDeck() {
        gameService.closeDeck();
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/replace-card")
    public ResponseEntity<Void> replaceCard() {
        gameService.replaceCard();
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/finish-deal")
    public ResponseEntity<Void> finishDeal() {
        gameService.finishDeal();
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/surrender")
    public ResponseEntity<Void> surrender() {
        gameService.surrender();
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/inactivity")
    public ResponseEntity<Void> inactivity() {
        gameService.inactivity();
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/extend-time")
    public ResponseEntity<Void> extendTime() {
        gameService.extendNextMoveTime();
        return ResponseEntity.accepted().build();
    }
}
