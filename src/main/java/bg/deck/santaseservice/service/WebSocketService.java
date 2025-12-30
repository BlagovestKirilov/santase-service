package bg.deck.santaseservice.service;

import bg.deck.santaseservice.model.response.GameStateResponse;
import bg.deck.santaseservice.model.response.SearchGameResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

import static bg.deck.santaseservice.constant.Constants.NOTIFY_GAME_DESTINATION;
import static bg.deck.santaseservice.constant.Constants.NOTIFY_GAME_SEARCH_DESTINATION;

@RequiredArgsConstructor
@Service
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    @Async
    public void notifyGameUpdate(String gameId, String username, GameStateResponse gameState) {
        String destination = String.format(NOTIFY_GAME_DESTINATION, gameId, username);
        messagingTemplate.convertAndSend(destination, gameState);
    }

    @Async
    public void notifyGameSearch(String username, SearchGameResponse searchGameResponse) {
        String destination = String.format(NOTIFY_GAME_SEARCH_DESTINATION, username);
        messagingTemplate.convertAndSend(destination, searchGameResponse);
    }

    @Async
    public void notifyGameSearch(List<String> usernames, SearchGameResponse searchGameResponse) {
        for (String username : usernames) {
            String destination = String.format(NOTIFY_GAME_SEARCH_DESTINATION, username);
            messagingTemplate.convertAndSend(destination, searchGameResponse);
        }
    }
}
