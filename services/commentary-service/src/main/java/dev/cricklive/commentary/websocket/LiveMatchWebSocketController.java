package dev.cricklive.commentary.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * STOMP WebSocket controller.
 *
 * Clients connect to: ws://host/ws
 * Subscribe to: /topic/match/{matchId}/live
 * Send to (optional ping): /app/match/{matchId}/ping
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class LiveMatchWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handles a client ping (keep-alive or initial subscription confirmation).
     * Responds to the same /topic channel so the client knows it's connected.
     */
    @MessageMapping("/match/{matchId}/ping")
    @SendTo("/topic/match/{matchId}/live")
    public Map<String, String> ping(@DestinationVariable String matchId) {
        log.debug("Client pinged matchId={}", matchId);
        return Map.of("type", "PONG", "matchId", matchId);
    }

    /**
     * Utility for internal use — push any payload to all subscribers of a match.
     */
    public void broadcastToMatch(String matchId, Object payload) {
        messagingTemplate.convertAndSend("/topic/match/" + matchId + "/live", payload);
    }
}
