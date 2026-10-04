package dev.cricklive.match.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/**
 * Ball-delivery event consumed from the {@code ball-events} Kafka topic (JSON).
 * Mirrors the record published by scoring-service; see its field documentation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BallEventMessage(
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
