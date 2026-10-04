package dev.cricklive.scoring.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

/** Event-store entry for every ball delivery. */
@Document(collection = BallEventDocument.COLLECTION)
@CompoundIndexes({
    @CompoundIndex(name = "innings_sequence", def = "{'inningsId': 1, 'sequence': 1}", unique = true),
    @CompoundIndex(name = "event_id", def = "{'eventId': 1}", unique = true),
    @CompoundIndex(name = "match_timestamp", def = "{'matchId': 1, 'timestamp': -1}")
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BallEventDocument {

    public static final String COLLECTION = "ball_events";

    @Id
    private String id;

    private String eventId;
    private long sequence;

    private UUID matchId;
    private UUID inningsId;

    private int overNumber;
    private int ballNumber;
    private boolean legalBall;
    private int legalBallsInInnings;

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
}
