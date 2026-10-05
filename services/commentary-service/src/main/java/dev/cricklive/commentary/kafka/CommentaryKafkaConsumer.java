package dev.cricklive.commentary.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.cricklive.commentary.domain.CommentaryDocument;
import dev.cricklive.commentary.domain.CommentaryRepository;
import dev.cricklive.commentary.dto.BallEventMessage;
import dev.cricklive.commentary.service.CommentaryGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Consumes ball events from Kafka, generates commentary, persists it to MongoDB and publishes it
 * to Redis Pub/Sub for WebSocket fan-out. Redelivered events are ignored via the ball event id.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CommentaryKafkaConsumer {

    private final CommentaryRepository repository;
    private final CommentaryGeneratorService generatorService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${cricklive.kafka.topics.ball-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void onBallEvent(BallEventMessage event) {
        if (repository.existsByBallEventId(event.eventId())) {
            log.debug("Commentary already exists for eventId={}", event.eventId());
            return;
        }

        CommentaryDocument doc = generatorService.generate(event);
        try {
            repository.save(doc);
        } catch (DuplicateKeyException dup) {
            log.debug("Commentary for eventId={} saved concurrently", event.eventId());
            return;
        }

        redisTemplate.convertAndSend("match:" + event.matchId() + ":commentary", payloadOf(doc));
        log.debug("Commentary generated matchId={} over={}.{} type={}",
                event.matchId(), event.overNumber(), event.ballNumber(), doc.getEventType());
    }

    private String payloadOf(CommentaryDocument doc) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "COMMENTARY");
        body.put("id", doc.getId());
        body.put("matchId", doc.getMatchId());
        body.put("inningsId", doc.getInningsId());
        body.put("over", doc.getOverNumber() + "." + doc.getBallNumber());
        body.put("commentary", doc.getText());
        body.put("eventType", doc.getEventType());
        body.put("timestamp", doc.getTimestamp());
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialise commentary update", ex);
        }
    }
}
