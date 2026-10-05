package dev.cricklive.scoring.kafka;

import dev.cricklive.scoring.dto.BallEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Publishes {@link BallEvent} JSON records to Kafka, keyed on matchId so every ball of a match
 * lands on one partition and is consumed in order.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BallEventProducer {

    private static final long ACK_TIMEOUT_SECONDS = 5;

    @Value("${cricklive.kafka.topics.ball-events:ball-events}")
    private String topic;

    private final KafkaTemplate<String, BallEvent> kafkaTemplate;

    /** Sends the event and waits for the broker acknowledgement. */
    public RecordMetadata publish(BallEvent event) throws Exception {
        RecordMetadata meta = kafkaTemplate.send(topic, event.matchId().toString(), event)
                .get(ACK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .getRecordMetadata();
        log.debug("Published ball matchId={} seq={} partition={} offset={}",
                event.matchId(), event.sequence(), meta.partition(), meta.offset());
        return meta;
    }
}
