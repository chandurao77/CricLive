package dev.cricklive.match.dto;

import dev.cricklive.match.domain.enums.MatchFormat;
import dev.cricklive.match.domain.enums.MatchStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Lightweight match summary for listings and carousels")
public record MatchSummaryDto(
        @Schema(description = "Match UUID") UUID id,
        MatchFormat format,
        MatchStatus status,
        String seriesName,
        TeamRefDto homeTeam,
        TeamRefDto awayTeam,
        String venueName,
        String venueCity,
        Instant scheduledStart,
        String statusText,
        InningsSummaryDto firstInnings,
        InningsSummaryDto secondInnings
) {}
