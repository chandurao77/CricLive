package dev.cricklive.commentary.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Listens on Redis Pub/Sub channels (published by match-service and commentary-service
 * Kafka consumer) and fans out to WebSocket clients via STOMP.
 *
 * Channel format: match:{matchId}:score  |  match:{matchId}:commentary
 * STOMP topic:    /topic/match/{matchId}/live
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScorecardRedisSubscriber implements MessageListener {

    private static final Pattern CHANNEL_PATTERN =
            Pattern.compile("match:([\\w-]+):(score|commentary)");

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);

        Matcher m = CHANNEL_PATTERN.matcher(channel);
        if (!m.matches()) {
            log.warn("Unexpected Redis channel: {}", channel);
            return;
        }

        String matchId = m.group(1);
        String type = m.group(2);  // "score" or "commentary"

        String destination = "/topic/match/" + matchId + "/live";
        messagingTemplate.convertAndSend(destination, payload);
        log.debug("Forwarded {} update for matchId={} to WebSocket clients", type, matchId);
    }
}
