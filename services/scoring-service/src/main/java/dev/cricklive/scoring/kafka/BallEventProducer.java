package dev.cricklive.scoring.kafka;

import dev.cricklive.scoring.avro.BallEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Publishes BallEvent Avro records to Kafka.
 * Keyed on matchId so all balls for a match land on the same partition,
 * preserving ordering within a match.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BallEventProducer {

    @Value("${cricklive.kafka.topics.ball-events:ball-events}")
    private String topic;

    private final KafkaTemplate<String, BallEvent> kafkaTemplate;

    /**
     * Sends a BallEvent asynchronously. Returns a future for the caller to
     * attach callbacks or await if synchronous confirmation is needed.
     */
    public CompletableFuture<RecordMetadata> send(BallEvent event) {
        ProducerRecord<String, BallEvent> record = new ProducerRecord<>(
                topic,
                event.getMatchId(),   // partition key → all balls for a match in order
                event
        );

        CompletableFuture<RecordMetadata> result = new CompletableFuture<>();

        kafkaTemplate.send(record).whenComplete((sendResult, ex) -> {
            if (ex != null) {
                log.error("Failed to publish BallEvent matchId={} over={}.{} error={}",
                        event.getMatchId(), event.getOverNumber(), event.getBallNumber(), ex.getMessage());
                result.completeExceptionally(ex);
            } else {
                RecordMetadata meta = sendResult.getRecordMetadata();
                log.debug("Published BallEvent matchId={} over={}.{} partition={} offset={}",
                        event.getMatchId(), event.getOverNumber(), event.getBallNumber(),
                        meta.partition(), meta.offset());
                result.complete(meta);
            }
        });

        return result;
    }
}
