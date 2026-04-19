package dev.cricklive.commentary.kafka;

import dev.cricklive.commentary.domain.CommentaryDocument;
import dev.cricklive.commentary.domain.CommentaryRepository;
import dev.cricklive.commentary.service.CommentaryGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumes BallEvent records from Kafka, generates commentary text,
 * persists to MongoDB, and publishes to Redis Pub/Sub for WebSocket fanout.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CommentaryKafkaConsumer {

    private final CommentaryRepository repository;
    private final CommentaryGeneratorService generatorService;
    private final StringRedisTemplate redisTemplate;

    @KafkaListener(
        topics = "${cricklive.kafka.topics.ball-events}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "ballEventListenerContainerFactory"
    )
    public void onBallEvent(
            @Payload Map<String, Object> event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        try {
            String matchId = (String) event.get("matchId");
            String inningsId = (String) event.get("inningsId");

            CommentaryDocument doc = generatorService.generate(event);
            repository.save(doc);

            String channel = "match:" + matchId + ":commentary";
            String payload = doc.getHtmlText() != null ? doc.getHtmlText() : doc.getText();
            redisTemplate.convertAndSend(channel, payload);

            log.debug("Commentary generated matchId={} over={}.{} type={}",
                    matchId, event.get("overNumber"), event.get("ballNumber"), doc.getEventType());

        } catch (Exception ex) {
            log.error("Failed to process BallEvent for commentary partition={} offset={}", partition, offset, ex);
        }
    }
}
