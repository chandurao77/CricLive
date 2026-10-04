package dev.cricklive.commentary.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/** Ball-delivery event consumed from the {@code ball-events} Kafka topic (JSON, see scoring-service). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BallEventMessage(
        String eventId,
        long sequence,
        UUID matchId,
        UUID inningsId,
        int overNumber,
        int ballNumber,
        boolean legalBall,
        int runsScored,
        boolean wicket,
        String dismissalType,
        boolean wide,
        boolean noBall,
        boolean bye,
        boolean legBye,
        int extraRuns,
        boolean boundary,
        boolean six,
        Instant timestamp
) {}
