package dev.cricklive.match.dto;

import dev.cricklive.match.domain.enums.MatchFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Payload to create a new match")
public record CreateMatchRequest(
        @NotNull UUID seriesId,
        Integer matchNumber,
        @NotNull MatchFormat format,
        @NotNull UUID venueId,
        @NotNull Instant scheduledStart,
        @NotNull UUID homeTeamId,
        @NotNull UUID awayTeamId
) {}
