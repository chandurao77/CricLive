package dev.cricklive.commentary.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * Configures STOMP over WebSocket.
 *
 * Client subscribes to: /topic/match/{matchId}/live
 * Server broadcasts to: /topic/match/{matchId}/live
 *
 * SockJS fallback is enabled so browsers without native WS still work.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Simple in-memory broker for /topic/** destinations
        registry.enableSimpleBroker("/topic");
        // Prefix for messages routed to @MessageMapping methods
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setHeartbeatTime(25_000)
                .setDisconnectDelay(5_000);
    }
}
