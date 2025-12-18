package com.bussiness.santaseservice.controller;

import com.bussiness.santaseservice.model.request.GameRequest;
import com.bussiness.santaseservice.model.request.PlayCardRequest;
import com.bussiness.santaseservice.service.GameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RequiredArgsConstructor
@RequestMapping("/game")
@RestController
public class GameController {

    private final GameService gameService;

    @PostMapping("/search")
    public ResponseEntity<Void> searchGame(@Valid @RequestBody GameRequest gameRequest) {
        log.info("Trying to start game, account with username {}", gameRequest.getUsername());
        gameService.searchGame(gameRequest.getUsername());
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/state")
    public ResponseEntity<Void> state(@RequestParam String username) {
        log.info("Trying to get game state id: {}", username);
        gameService.getGameState(username);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/play-card")
    public ResponseEntity<Void> playCard(@RequestBody PlayCardRequest playCardRequest) {
        log.info("Trying to play card: {}", playCardRequest);
        gameService.playCard(playCardRequest);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/close-deck")
    public ResponseEntity<Void> closeDeck(@RequestBody GameRequest gameRequest) {
        log.info("Trying to close deck: {}", gameRequest);
        gameService.closeDeck(gameRequest);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/replace-card")
    public ResponseEntity<Void> replaceCard(@RequestBody GameRequest gameRequest) {
        log.info("Trying to replace card: {}", gameRequest);
        gameService.replaceCard(gameRequest);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/finish-deal")
    public ResponseEntity<Void> finishDeal(@RequestBody GameRequest gameRequest) {
        log.info("Trying to finish deal: {}", gameRequest);
        gameService.finishDeal(gameRequest);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/finish-game")
    public ResponseEntity<Void> finishGame(@RequestBody GameRequest gameRequest) {
        log.info("Trying to finish game: {}", gameRequest);
        gameService.finishGame(gameRequest);
        return ResponseEntity.accepted().build();
    }
}
