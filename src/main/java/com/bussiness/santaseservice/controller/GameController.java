package com.bussiness.santaseservice.controller;

import com.bussiness.santaseservice.model.Game;
import com.bussiness.santaseservice.model.GameState;
import com.bussiness.santaseservice.model.request.CloseDeckRequest;
import com.bussiness.santaseservice.model.request.FinishDealRequest;
import com.bussiness.santaseservice.model.request.PlayCardRequest;
import com.bussiness.santaseservice.model.request.ReplaceCardRequest;
import com.bussiness.santaseservice.model.request.SearchGameRequest;
import com.bussiness.santaseservice.model.response.PlayCardResponse;
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

import java.util.UUID;

@Log4j2
@RequiredArgsConstructor
@RequestMapping("/game")
@RestController
public class GameController {

    private final GameService gameService;

    @PostMapping("/search")
    public ResponseEntity<Game> searchGame(@Valid @RequestBody SearchGameRequest searchGameRequest) {
        log.info("Trying to start game, account with username {}", searchGameRequest.getUsername());
        return ResponseEntity.ok(gameService.searchGame(searchGameRequest.getUsername()));
    }

    @GetMapping("/state")
    public ResponseEntity<GameState> state(@RequestParam UUID gameId) {
        log.info("Trying to get game state id: {}", gameId);
        return ResponseEntity.ok(gameService.getGameState(gameId));
    }

    @PostMapping("/play-card")
    public ResponseEntity<PlayCardResponse> playCard(@RequestBody PlayCardRequest playCardRequest) {
        log.info("Trying to play card: {}", playCardRequest);
        return ResponseEntity.ok(gameService.playCard(playCardRequest));
    }

    @PostMapping("/close-deck")
    public ResponseEntity<GameState> closeDeck(@RequestBody CloseDeckRequest closeDeckRequest) {
        log.info("Trying to close deck: {}", closeDeckRequest);
        return ResponseEntity.ok(gameService.closeDeck(closeDeckRequest));
    }

    @PostMapping("/replace-card")
    public ResponseEntity<GameState> replaceCard(@RequestBody ReplaceCardRequest replaceCardRequest) {
        log.info("Trying to replace card: {}", replaceCardRequest);
        return ResponseEntity.ok(gameService.replaceCard(replaceCardRequest));
    }

    @PostMapping("/finish-deal")
    public ResponseEntity<GameState> finishDeal(@RequestBody FinishDealRequest finishDealRequest) {
        log.info("Trying to finish deal: {}", finishDealRequest);
        return ResponseEntity.ok(gameService.finishDeal(finishDealRequest));
    }
}
