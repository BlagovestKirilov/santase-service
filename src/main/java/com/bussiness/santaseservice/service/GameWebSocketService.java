package com.bussiness.santaseservice.service;

import com.bussiness.santaseservice.model.Game;
import com.bussiness.santaseservice.model.GameState;
import com.bussiness.santaseservice.model.dto.CardDTO;
import com.bussiness.santaseservice.model.response.GameStateResponse;
import com.bussiness.santaseservice.util.CardMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class GameWebSocketService {
    private final SimpMessagingTemplate messagingTemplate;
    private final CardMapper cardMapper;

    public GameStateResponse updateGameState(Game game, String username) {
        boolean isFirstPlayer = game.getFirstPlayer().getUsername().equals(username);

        GameState gameState = game.getState();

        CardDTO trumpCardDTO = cardMapper.toDTO(gameState.getTrumpCard());
        CardDTO playedCardDTO = isFirstPlayer
                ? cardMapper.toDTO(gameState.getFirstPlayerPlayedCard())
                : cardMapper.toDTO(gameState.getSecondPlayerPlayedCard());
        CardDTO opponentPlayedCardDTO = isFirstPlayer
                ? cardMapper.toDTO(gameState.getSecondPlayerPlayedCard())
                : cardMapper.toDTO(gameState.getFirstPlayerPlayedCard());

        List<CardDTO> deckDTO = isFirstPlayer
                ? cardMapper.toDTO(gameState.getFirstPlayerHand())
                : cardMapper.toDTO(gameState.getSecondPlayerHand());

        GameStateResponse response = GameStateResponse.builder()
                .gameId(game.getId())
                .deck(deckDTO)
                .trumpCard(trumpCardDTO)
                .playedCard(playedCardDTO)
                .opponentPlayedCard(opponentPlayedCardDTO)
                .firstPlayerUsername(game.getFirstPlayer().getUsername())
                .firstPlayerResult(game.getFirstPlayerResult())
                .secondPlayerUsername(game.getSecondPlayer().getUsername())
                .secondPlayerResult(game.getSecondPlayerResult())
                .remainingCardsCount(gameState.getDeck().size())
                .isOnTurn(username.equals(gameState.getInTurnPlayerUsername()))
                .build();

        notifyGameUpdate(game.getId().toString(), username, response);

        return response;
    }

    public void notifyGameUpdate(String gameId, String username, GameStateResponse gameState) {
        // The destination uses the /topic prefix defined in WebSocketConfig
        // Clients subscribe to "/topic/game/{gameId}"
        String destination = "/topic/game/" + gameId + "/" + username;

        System.out.println("Pushing update to destination: " + destination);
        System.out.println("Payload: " + gameState.toString());

        // This is the line that pushes the message out to all connected clients
        messagingTemplate.convertAndSend(destination, gameState);
    }
}
