package dev.cricklive.scoring.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable ball-delivery event published to the {@code ball-events} Kafka topic as JSON.
 *
 * <p>{@code sequence} is a per-innings, strictly increasing number (gaps are possible after a
 * failed publish). Consumers use it to ignore duplicates: apply only when it is greater than the
 * last sequence they applied for that innings.
 *
 * <p>{@code overNumber} is 0-indexed. For a legal delivery {@code ballNumber} is 1..6 (its place in
 * the over). For a wide or no-ball, {@code ballNumber} is the count of legal balls already bowled
 * in that over (0 means before the first legal ball).
 */
public record BallEvent(
        String eventId,
        long sequence,
        UUID matchId,
        UUID inningsId,
        int overNumber,
        int ballNumber,
        boolean legalBall,
        int legalBallsInInnings,
        UUID batterId,
        UUID bowlerId,
        int runsScored,
        boolean wicket,
        String dismissalType,
        UUID dismissedBatterId,
        UUID fielderId,
        boolean wide,
        boolean noBall,
        boolean bye,
        boolean legBye,
        int extraRuns,
        boolean boundary,
        boolean six,
        Instant timestamp,
        UUID scorerId
) {}
