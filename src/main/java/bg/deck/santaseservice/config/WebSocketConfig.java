package bg.deck.santaseservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import static bg.deck.santaseservice.constant.Constants.APP;
import static bg.deck.santaseservice.constant.Constants.DECK_BG;
import static bg.deck.santaseservice.constant.Constants.LOCALHOST;
import static bg.deck.santaseservice.constant.Constants.PROD;
import static bg.deck.santaseservice.constant.Constants.TOPIC;
import static bg.deck.santaseservice.constant.Constants.WEB_SOCKET_ENDPOINT;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${spring.profiles.active}")
    private String activeProfile;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 1. Prefix for messages sent *to* the client from the server (the push updates)
        config.enableSimpleBroker(TOPIC);

        // 2. Prefix for messages sent *from* the client *to* the server (e.g., player moves)
        config.setApplicationDestinationPrefixes(APP);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(WEB_SOCKET_ENDPOINT)
                .setAllowedOriginPatterns(activeProfile.equals(PROD) ? DECK_BG : LOCALHOST)
                .withSockJS();
    }
}
