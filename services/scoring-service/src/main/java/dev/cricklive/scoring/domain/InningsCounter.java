package dev.cricklive.scoring.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Atomic per-innings counters used to number deliveries. Document id is the innings UUID. */
@Document(collection = InningsCounter.COLLECTION)
@Getter
@NoArgsConstructor
public class InningsCounter {

    public static final String COLLECTION = "innings_counters";

    @Id
    private String id;

    /** Number of events recorded so far (legal or not). */
    private long seq;

    /** Number of legal deliveries recorded so far. */
    private int legalBalls;
}
