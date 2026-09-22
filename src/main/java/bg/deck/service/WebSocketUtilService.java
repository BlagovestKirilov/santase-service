package bg.deck.service;

import bg.deck.model.Game;
import bg.deck.model.GameState;
import bg.deck.model.Player;
import bg.deck.model.dto.CardDTO;
import bg.deck.model.response.GameStateResponse;
import bg.deck.util.CardMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@RequiredArgsConstructor
@Service
public class WebSocketUtilService {
    private final WebSocketService webSocketService;
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

        webSocketService.notifyGameUpdate(game.getId().toString(), username, response);
    }

    public void updateGameState(Game game) {
        List<String> players = List.of(game.getFirstPlayer().getUsername(),
                game.getSecondPlayer().getUsername());

        for (String player : players) {
            GameStateResponse response = buildBaseGameStateResponse(game, player);
            webSocketService.notifyGameUpdate(game.getId().toString(), player, response);
        }
    }

    public void updateGameStateWithTrickWinner(Game game, String trickWinner) {
        List<Player> players = List.of(game.getFirstPlayer(), game.getSecondPlayer());

        for (Player player : players) {
            String username = player.getUsername();

            GameStateResponse response = buildBaseGameStateResponse(game, username)
                    .toBuilder()
                    .trickWinnerUsername(trickWinner)
                    .trickFirstPlayerScore(game.getFirstPlayer().getScore())
                    .trickSecondPlayerScore(game.getSecondPlayer().getScore())
                    .build();

            webSocketService.notifyGameUpdate(game.getId().toString(), username, response);
        }
    }

    public void updateGameState(Game game, String username) {
        GameStateResponse response = buildBaseGameStateResponse(game, username);
        webSocketService.notifyGameUpdate(game.getId().toString(), username, response);
    }


    private GameStateResponse buildBaseGameStateResponse(Game game, String username) {
        Player player = game.getPlayerByUsername(username);
        Player opponentPlayer = game.getOpponent(player);

        GameState state = game.getState();

        CardDTO playedCard = cardMapper.toDTO(player.getPlayedCard());

        CardDTO opponentPlayedCard = cardMapper.toDTO(opponentPlayer.getPlayedCard());

        List<CardDTO> deck = cardMapper.toDTO(player.getHand());

        return GameStateResponse.builder()
                .gameId(game.getId().toString())
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
                .isOnTurn(state.isInTurn(player))
                .isClosed(state.isClosed())
                .winnerUsername(game.getWinner() != null ? game.getWinner().getUsername() : null)
                .surrenderPlayerUsername(game.getSurrenderPlayer() != null ? game.getSurrenderPlayer().getUsername() : null)
                .bonus(player.getBonus())
                .opponentPlayerBonus(opponentPlayer.getBonus())
                .inactivityCount(player.getInactivityCount())
                .nextMoveTimeInSeconds(state.isInTurn(player) ?
                        Math.toIntExact(Duration.between(Instant.now(), state.getNextMoveTime()).getSeconds()) : null)
                .build();
    }
}
