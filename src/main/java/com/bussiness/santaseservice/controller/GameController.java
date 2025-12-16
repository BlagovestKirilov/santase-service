package com.bussiness.santaseservice.controller;

import com.bussiness.santaseservice.model.request.CloseDeckRequest;
import com.bussiness.santaseservice.model.request.FinishDealRequest;
import com.bussiness.santaseservice.model.request.PlayCardRequest;
import com.bussiness.santaseservice.model.request.ReplaceCardRequest;
import com.bussiness.santaseservice.model.request.SearchGameRequest;
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
    public ResponseEntity<Void> searchGame(@Valid @RequestBody SearchGameRequest searchGameRequest) {
        log.info("Trying to start game, account with username {}", searchGameRequest.getUsername());
        gameService.searchGame(searchGameRequest.getUsername());
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/state")
    public ResponseEntity<Void> state(@RequestParam UUID gameId, @RequestParam String username) {
        log.info("Trying to get game state id: {}", gameId);
        gameService.getGameState(gameId, username);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/play-card")
    public ResponseEntity<Void> playCard(@RequestBody PlayCardRequest playCardRequest) {
        log.info("Trying to play card: {}", playCardRequest);
        gameService.playCard(playCardRequest);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/close-deck")
    public ResponseEntity<Void> closeDeck(@RequestBody CloseDeckRequest closeDeckRequest) {
        log.info("Trying to close deck: {}", closeDeckRequest);
        gameService.closeDeck(closeDeckRequest);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/replace-card")
    public ResponseEntity<Void> replaceCard(@RequestBody ReplaceCardRequest replaceCardRequest) {
        log.info("Trying to replace card: {}", replaceCardRequest);
        gameService.replaceCard(replaceCardRequest);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/finish-deal")
    public ResponseEntity<Void> finishDeal(@RequestBody FinishDealRequest finishDealRequest) {
        log.info("Trying to finish deal: {}", finishDealRequest);
        gameService.finishDeal(finishDealRequest);
        return ResponseEntity.accepted().build();
    }
}
