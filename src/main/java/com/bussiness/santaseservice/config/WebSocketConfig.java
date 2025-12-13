package com.bussiness.santaseservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 1. Prefix for messages sent *to* the client from the server (the push updates)
        config.enableSimpleBroker("/topic");

        // 2. Prefix for messages sent *from* the client *to* the server (e.g., player moves)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // The endpoint clients will use to initiate the WebSocket connection
        // Allows connections from any origin (important for development)
        registry.addEndpoint("/ws-game").setAllowedOriginPatterns("*").withSockJS();
    }
}
