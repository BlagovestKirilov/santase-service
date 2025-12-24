package bg.deck.santaseservice.controller;

import bg.deck.santaseservice.model.request.CardRequest;
import bg.deck.santaseservice.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RequiredArgsConstructor
@RequestMapping("/game")
@RestController
public class GameController {

    private final GameService gameService;

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
    public ResponseEntity<Void> playCard(@RequestBody CardRequest cardRequest) {
        log.info("Trying to play card: {}", cardRequest);
        gameService.playCard(cardRequest);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/announce")
    public ResponseEntity<Void> announceCombination(@RequestBody CardRequest cardRequest) {
        log.info("Trying to announce combination: {}", cardRequest);
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

    @PostMapping("/finish-game")
    public ResponseEntity<Void> finishGame() {
        gameService.finishGame();
        return ResponseEntity.accepted().build();
    }
}
