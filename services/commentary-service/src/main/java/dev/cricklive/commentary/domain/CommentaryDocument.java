package dev.cricklive.commentary.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Stores commentary text for every ball delivery.
 * Written by the commentary consumer when a BallEvent arrives from Kafka.
 */
@Document(collection = "commentary")
@CompoundIndexes({
    @CompoundIndex(name = "match_innings_over_ball",
                   def = "{'matchId': 1, 'inningsId': 1, 'overNumber': 1, 'ballNumber': 1}"),
    @CompoundIndex(name = "match_timestamp",
                   def = "{'matchId': 1, 'timestamp': -1}")
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentaryDocument {

    @Id
    private String id;

    private UUID matchId;
    private UUID inningsId;
    private int overNumber;
    private int ballNumber;

    /** Reference to the source ball event for traceability. */
    @Indexed(unique = true)
    private String ballEventId;

    private String text;
    private String htmlText;

    private CommentaryEventType eventType;

    private List<String> tags;

    private String language;

    private Instant timestamp;

    /** "system" for auto-generated, or scorer UUID for manual commentary. */
    private String author;

    public enum CommentaryEventType {
        NORMAL, BOUNDARY, SIX, WICKET, WIDE, NO_BALL, MAIDEN_OVER,
        FIFTY, CENTURY, FIFTY_PARTNERSHIP, HUNDRED_PARTNERSHIP, MILESTONE,
        INNINGS_START, INNINGS_END, MATCH_START, MATCH_END, RAIN_DELAY
    }
}
