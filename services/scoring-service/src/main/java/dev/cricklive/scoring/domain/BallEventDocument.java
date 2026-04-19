package dev.cricklive.scoring.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable event store entry for every ball delivery.
 * Written once, never mutated — corrections are new compensating events.
 */
@Document(collection = "ball_events")
@CompoundIndexes({
    @CompoundIndex(name = "match_innings_over_ball",
                   def = "{'matchId': 1, 'inningsId': 1, 'overNumber': 1, 'ballNumber': 1}",
                   unique = true),
    @CompoundIndex(name = "match_timestamp",
                   def = "{'matchId': 1, 'timestamp': -1}")
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BallEventDocument {

    @Id
    private String id;

    private String eventId;         // UUID idempotency key

    private UUID matchId;
    private UUID inningsId;

    private int overNumber;
    private int ballNumber;

    private UUID batterId;
    private UUID bowlerId;

    private int runsScored;

    private boolean wicket;
    private String dismissalType;
    private UUID dismissedBatterId;
    private UUID fielderId;

    private boolean wide;
    private boolean noBall;
    private boolean bye;
    private boolean legBye;
    private int extraRuns;

    private boolean boundary;
    private boolean six;

    private Instant timestamp;
    private UUID scorerId;

    /** Running innings state snapshot at the time of this ball. */
    private InningsSnapshot inningsSnapshot;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InningsSnapshot {
        private int totalRuns;
        private int wickets;
        private String overs;
        private double runRate;
    }
}
