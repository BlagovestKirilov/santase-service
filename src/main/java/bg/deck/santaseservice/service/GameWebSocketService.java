package bg.deck.santaseservice.service;

import bg.deck.santaseservice.model.Game;
import bg.deck.santaseservice.model.GameState;
import bg.deck.santaseservice.model.Player;
import bg.deck.santaseservice.model.dto.CardDTO;
import bg.deck.santaseservice.model.response.GameStateResponse;
import bg.deck.santaseservice.model.response.SearchGameResponse;
import bg.deck.santaseservice.util.CardMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class GameWebSocketService {
    private final SimpMessagingTemplate messagingTemplate;
    private final CardMapper cardMapper;

    public void updateGameState(
            Game game,
            String username,
            String trickWinnerUsername,
            int trickFirstPlayerScore,
            int trickSecondPlayerScore
    ) {
        GameStateResponse response = buildBaseGameStateResponse(game, username)
                .toBuilder()
                .trickWinnerUsername(trickWinnerUsername)
                .trickFirstPlayerScore(trickFirstPlayerScore)
                .trickSecondPlayerScore(trickSecondPlayerScore)
                .build();

        notifyGameUpdate(game.getId().toString(), username, response);
    }

    public void updateGameState(Game game, String username) {
        GameStateResponse response = buildBaseGameStateResponse(game, username);

        notifyGameUpdate(game.getId().toString(), username, response);
    }

    public void notifyGameUpdate(String gameId, String username, GameStateResponse gameState) {
        String destination = "/topic/game/" + gameId + "/" + username;

        messagingTemplate.convertAndSend(destination, gameState);
    }

    public void notifyGameSearch(String username, SearchGameResponse searchGameResponse) {
        String destination = "/topic/game/" + username;

        messagingTemplate.convertAndSend(destination, searchGameResponse);
    }

    private GameStateResponse buildBaseGameStateResponse(Game game, String username) {
        Player player = game.getPlayerByUsername(username);
        Player opponentPlayer = game.getOpponent(player);

        GameState state = game.getState();

        CardDTO playedCard = cardMapper.toDTO(player.getPlayedCard());

        CardDTO opponentPlayedCard = cardMapper.toDTO(opponentPlayer.getPlayedCard());

        List<CardDTO> deck = cardMapper.toDTO(player.getHand());

        return GameStateResponse.builder()
                .deck(deck)
                .trumpCard(cardMapper.toDTO(state.getTrumpCard()))
                .playedCard(playedCard)
                .opponentPlayedCard(opponentPlayedCard)
                .opponentPlayerCardsCount(opponentPlayer.getHand().size())
                .firstPlayerUsername(game.getFirstPlayer().getUsername())
                .firstPlayerResult(game.getFirstPlayer().getResult())
                .secondPlayerUsername(game.getSecondPlayer().getUsername())
                .secondPlayerResult(game.getSecondPlayer().getResult())
                .remainingCardsCount(state.getDeck().size())
                .isOnTurn(username.equals(state.getInTurnPlayer().getUsername()))
                .isClosed(state.isClosed())
                .winnerUsername(game.getWinner() != null ? game.getWinner().getUsername() : null)
                .bonus(player.getBonus())
                .opponentPlayerBonus(opponentPlayer.getBonus())
                .build();
    }
}
