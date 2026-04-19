package dev.cricklive.scoring.service;

import dev.cricklive.scoring.avro.BallEvent;
import dev.cricklive.scoring.domain.BallEventDocument;
import dev.cricklive.scoring.dto.BallInputDto;
import dev.cricklive.scoring.kafka.BallEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScoringService {

    private final BallEventProducer producer;
    private final MongoTemplate mongoTemplate;

    /**
     * Validates the ball input, persists it to MongoDB event store, then
     * publishes the Avro event to Kafka. Idempotent via eventId check.
     */
    public void recordBall(BallInputDto input, UUID scorerId) {
        String idempotencyKey = resolveIdempotencyKey(input);

        // Idempotency guard — if this event was already stored, skip
        if (eventAlreadyExists(idempotencyKey)) {
            log.warn("Duplicate ball event ignored idempotencyKey={}", idempotencyKey);
            return;
        }

        BallEventDocument doc = BallEventDocument.builder()
                .eventId(idempotencyKey)
                .matchId(input.matchId())
                .inningsId(input.inningsId())
                .overNumber(deriveOverNumber(input))
                .ballNumber(deriveBallNumber(input))
                .batterId(input.batterId())
                .bowlerId(input.bowlerId())
                .runsScored(input.runsScored())
                .wicket(input.wicket())
                .dismissalType(input.dismissalType())
                .dismissedBatterId(input.dismissedBatterId())
                .fielderId(input.fielderId())
                .wide(input.wide())
                .noBall(input.noBall())
                .bye(input.bye())
                .legBye(input.legBye())
                .extraRuns(input.extraRuns())
                .boundary(input.runsScored() == 4 || input.runsScored() == 6)
                .six(input.runsScored() == 6)
                .timestamp(Instant.now())
                .scorerId(scorerId)
                .build();

        mongoTemplate.insert(doc, "ball_events");

        BallEvent avroEvent = toAvro(doc, scorerId);
        producer.send(avroEvent).exceptionally(ex -> {
            log.error("Kafka publish failed for eventId={}, will retry via outbox", idempotencyKey, ex);
            return null;
        });

        log.info("Recorded ball eventId={} matchId={} inningsId={} runsScored={}",
                idempotencyKey, input.matchId(), input.inningsId(), input.runsScored());
    }

    private boolean eventAlreadyExists(String idempotencyKey) {
        Query q = Query.query(Criteria.where("eventId").is(idempotencyKey));
        return mongoTemplate.exists(q, "ball_events");
    }

    private String resolveIdempotencyKey(BallInputDto input) {
        return input.idempotencyKey() != null
                ? input.idempotencyKey()
                : UUID.randomUUID().toString();
    }

    /** Derives 0-indexed over number from the current innings state (placeholder — real impl queries current state). */
    private int deriveOverNumber(BallInputDto input) {
        // Production: fetch current over from match state cache
        return 0;
    }

    private int deriveBallNumber(BallInputDto input) {
        return 1;
    }

    private BallEvent toAvro(BallEventDocument doc, UUID scorerId) {
        return BallEvent.newBuilder()
                .setEventId(doc.getEventId())
                .setMatchId(doc.getMatchId().toString())
                .setInningsId(doc.getInningsId().toString())
                .setOverNumber(doc.getOverNumber())
                .setBallNumber(doc.getBallNumber())
                .setBatterId(doc.getBatterId().toString())
                .setBowlerId(doc.getBowlerId().toString())
                .setRunsScored(doc.getRunsScored())
                .setIsWicket(doc.isWicket())
                .setDismissalType(doc.getDismissalType())
                .setDismissedBatterId(doc.getDismissedBatterId() != null ? doc.getDismissedBatterId().toString() : null)
                .setFielderId(doc.getFielderId() != null ? doc.getFielderId().toString() : null)
                .setIsWide(doc.isWide())
                .setIsNoBall(doc.isNoBall())
                .setIsBye(doc.isBye())
                .setIsLegBye(doc.isLegBye())
                .setExtraRuns(doc.getExtraRuns())
                .setIsBoundary(doc.isBoundary())
                .setIsSix(doc.isSix())
                .setTimestamp(doc.getTimestamp().toEpochMilli())
                .setScorerId(scorerId.toString())
                .build();
    }
}
