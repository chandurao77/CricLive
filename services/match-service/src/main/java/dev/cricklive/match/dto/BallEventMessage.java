package dev.cricklive.match.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Internal representation of a BallEvent consumed from Kafka (deserialized from Avro).
 */
public record BallEventMessage(
        String eventId,
        UUID matchId,
        UUID inningsId,
        int overNumber,
        int ballNumber,
        UUID batterId,
        UUID bowlerId,
        int runsScored,
        boolean isWicket,
        String dismissalType,
        boolean isWide,
        boolean isNoBall,
        boolean isBye,
        boolean isLegBye,
        int extraRuns,
        Instant timestamp,
        UUID scorerId
) {}
