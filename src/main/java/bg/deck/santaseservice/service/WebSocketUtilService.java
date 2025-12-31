package bg.deck.santaseservice.service;

import bg.deck.santaseservice.config.TransactionalWebSocketDispatcher;
import bg.deck.santaseservice.model.Game;
import bg.deck.santaseservice.model.GameState;
import bg.deck.santaseservice.model.Player;
import bg.deck.santaseservice.model.dto.CardDTO;
import bg.deck.santaseservice.model.response.GameStateResponse;
import bg.deck.santaseservice.util.CardMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class WebSocketUtilService {
    private final WebSocketService webSocketService;
    private final CardMapper cardMapper;
    private final TransactionalWebSocketDispatcher dispatcher;

    public void updateGameState(
            Game game,
            String username,
            String trickWinnerUsername,
            int trickFirstPlayerScore,
            int trickSecondPlayerScore
    ) {
        dispatcher.send(() -> {
            GameStateResponse response = buildBaseGameStateResponse(game, username)
                    .toBuilder()
                    .trickWinnerUsername(trickWinnerUsername)
                    .trickFirstPlayerScore(trickFirstPlayerScore)
                    .trickSecondPlayerScore(trickSecondPlayerScore)
                    .build();

            webSocketService.notifyGameUpdate(game.getId().toString(), username, response);
        });
    }

    public void updateGameState(Game game) {
        dispatcher.send(() -> {
            List<String> players = List.of(
                    game.getFirstPlayer().getUsername(),
                    game.getSecondPlayer().getUsername());

            for (String player : players) {
                GameStateResponse response = buildBaseGameStateResponse(game, player);
                webSocketService.notifyGameUpdate(game.getId().toString(), player, response);
            }
        });
    }

    public void updateGameStateWithTrickWinner(Game game, String trickWinner) {
        dispatcher.send(() -> {
            List<Player> players = List.of(game.getFirstPlayer(), game.getSecondPlayer());

            for (Player player : players) {
                String username = player.getUsername();

                GameStateResponse response = buildBaseGameStateResponse(game, username);

                response.setTrickWinnerUsername(trickWinner);
                response.setTrickFirstPlayerScore(game.getFirstPlayer().getScore());
                response.setTrickSecondPlayerScore(game.getSecondPlayer().getScore());

                webSocketService.notifyGameUpdate(game.getId().toString(), username, response);
            }
        });
    }

    public void updateGameState(Game game, String username) {
        dispatcher.send(() -> {
            GameStateResponse response = buildBaseGameStateResponse(game, username);
            webSocketService.notifyGameUpdate(game.getId().toString(), username, response);
        });
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
