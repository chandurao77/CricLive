package dev.cricklive.scoring.service;

import dev.cricklive.scoring.domain.BallEventDocument;
import dev.cricklive.scoring.domain.InningsCounter;
import dev.cricklive.scoring.dto.BallAck;
import dev.cricklive.scoring.dto.BallEvent;
import dev.cricklive.scoring.dto.BallInputDto;
import dev.cricklive.scoring.exception.EventPublishException;
import dev.cricklive.scoring.kafka.BallEventProducer;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScoringService {

    private static final int SIX = 6;
    private static final int FOUR = 4;

    private final BallEventProducer producer;
    private final MongoTemplate mongoTemplate;

    /**
     * Validates a delivery, numbers it, stores it in the event store, then publishes it to Kafka and
     * waits for the acknowledgement. If publishing fails the stored event is removed again so the
     * scorer can safely retry with the same idempotency key.
     *
     * @param input    the scorer's input
     * @param scorerId id of the authenticated scorer
     * @return acknowledgement with the assigned sequence and over.ball position
     */
    public BallAck recordBall(BallInputDto input, UUID scorerId) {
        BallRules.validate(input);

        String eventId = input.idempotencyKey() != null && !input.idempotencyKey().isBlank()
                ? input.idempotencyKey()
                : UUID.randomUUID().toString();

        BallEventDocument existing = findByEventId(eventId);
        if (existing != null) {
            log.warn("Duplicate ball ignored eventId={}", eventId);
            return ackOf(existing, true);
        }

        boolean legal = BallRules.isLegal(input);
        InningsCounter counter = nextCounter(input.inningsId(), legal);
        BallEventDocument doc = buildDocument(input, scorerId, eventId, counter, legal);

        try {
            mongoTemplate.insert(doc);
        } catch (DuplicateKeyException dup) {
            // A concurrent request with the same idempotency key won the race.
            compensate(input.inningsId(), legal);
            return ackOf(findByEventId(eventId), true);
        }

        try {
            producer.publish(toEvent(doc));
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            mongoTemplate.remove(Query.query(Criteria.where("eventId").is(eventId)), BallEventDocument.class);
            compensate(input.inningsId(), legal);
            log.error("Kafka publish failed, ball not recorded eventId={}", eventId, ex);
            throw new EventPublishException("Kafka publish failed for " + eventId, ex);
        }

        log.info("Recorded ball eventId={} matchId={} innings={} over={}.{} runs={}",
                eventId, doc.getMatchId(), doc.getInningsId(), doc.getOverNumber(), doc.getBallNumber(), doc.getRunsScored());
        return ackOf(doc, false);
    }

    private BallEventDocument findByEventId(String eventId) {
        return mongoTemplate.findOne(Query.query(Criteria.where("eventId").is(eventId)), BallEventDocument.class);
    }

    /** Atomically bumps the innings counters and returns the new values. */
    private InningsCounter nextCounter(UUID inningsId, boolean legal) {
        Document updated = mongoTemplate.execute(InningsCounter.COLLECTION, collection ->
                collection.findOneAndUpdate(
                        Filters.eq("_id", inningsId.toString()),
                        Updates.combine(Updates.inc("seq", 1L), Updates.inc("legalBalls", legal ? 1 : 0)),
                        new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)));
        return mongoTemplate.getConverter().read(InningsCounter.class, updated);
    }

    /** Undoes the legal-ball increment of a delivery that was not kept. Sequence numbers may gap. */
    private void compensate(UUID inningsId, boolean legal) {
        if (legal) {
            mongoTemplate.updateFirst(
                    Query.query(Criteria.where("_id").is(inningsId.toString())),
                    new Update().inc("legalBalls", -1),
                    InningsCounter.class);
        }
    }

    private BallEventDocument buildDocument(BallInputDto in, UUID scorerId, String eventId,
                                            InningsCounter counter, boolean legal) {
        int legalAfter = counter.getLegalBalls();
        return BallEventDocument.builder()
                .eventId(eventId)
                .sequence(counter.getSeq())
                .matchId(in.matchId())
                .inningsId(in.inningsId())
                .overNumber(BallRules.overNumber(legalAfter, legal))
                .ballNumber(BallRules.ballNumber(legalAfter, legal))
                .legalBall(legal)
                .legalBallsInInnings(legalAfter)
                .batterId(in.batterId())
                .bowlerId(in.bowlerId())
                .runsScored(in.runsScored())
                .wicket(in.wicket())
                .dismissalType(in.wicket() ? in.dismissalType().toUpperCase() : null)
                .dismissedBatterId(in.wicket() && in.dismissedBatterId() == null ? in.batterId() : in.dismissedBatterId())
                .fielderId(in.fielderId())
                .wide(in.wide())
                .noBall(in.noBall())
                .bye(in.bye())
                .legBye(in.legBye())
                .extraRuns(BallRules.normalizedExtraRuns(in))
                .boundary(in.runsScored() == FOUR || in.runsScored() == SIX)
                .six(in.runsScored() == SIX)
                .timestamp(Instant.now())
                .scorerId(scorerId)
                .build();
    }

    private BallEvent toEvent(BallEventDocument d) {
        return new BallEvent(d.getEventId(), d.getSequence(), d.getMatchId(), d.getInningsId(),
                d.getOverNumber(), d.getBallNumber(), d.isLegalBall(), d.getLegalBallsInInnings(),
                d.getBatterId(), d.getBowlerId(), d.getRunsScored(), d.isWicket(), d.getDismissalType(),
                d.getDismissedBatterId(), d.getFielderId(), d.isWide(), d.isNoBall(), d.isBye(), d.isLegBye(),
                d.getExtraRuns(), d.isBoundary(), d.isSix(), d.getTimestamp(), d.getScorerId());
    }

    private BallAck ackOf(BallEventDocument d, boolean duplicate) {
        return new BallAck(d.getEventId(), d.getSequence(), d.getOverNumber(), d.getBallNumber(), d.isLegalBall(), duplicate);
    }
}
