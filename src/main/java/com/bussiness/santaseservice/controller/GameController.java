package com.bussiness.santaseservice.controller;

import com.bussiness.santaseservice.model.Game;
import com.bussiness.santaseservice.model.GameState;
import com.bussiness.santaseservice.model.request.PlayCardRequest;
import com.bussiness.santaseservice.model.request.StartGameRequest;
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

    @PostMapping("/start")
    public ResponseEntity<Game> start(@Valid @RequestBody StartGameRequest startGameRequest) {
        log.info("Trying to start game, account with username {}", startGameRequest.getUsername());
        return ResponseEntity.ok(gameService.startGame(startGameRequest.getUsername()));
    }

    @GetMapping("/state")
    public ResponseEntity<GameState> state(@RequestParam Long gameId) {
        log.info("Trying to get game state id: {}", gameId);
        return ResponseEntity.ok(gameService.getGameState(gameId));
    }

    @PostMapping("/play-card")
    public ResponseEntity<GameState> playCard(@RequestBody PlayCardRequest playCardRequest) {
        log.info("Trying to play card: {}", playCardRequest);
        return ResponseEntity.ok(gameService.playCard(playCardRequest));
    }
}
