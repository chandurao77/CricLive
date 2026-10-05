package dev.cricklive.match.dto;

import dev.cricklive.match.domain.enums.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Full match detail including scorecard and innings breakdown")
public record MatchDetailDto(
        UUID id,
        MatchFormat format,
        MatchStatus status,
        String statusText,
        String seriesName,
        TeamRefDto homeTeam,
        TeamRefDto awayTeam,
        VenueDto venue,
        Instant scheduledStart,
        Instant actualStart,
        TossDto toss,
        ResultDto result,
        boolean dlsApplied,
        String notes,
        List<InningsDetailDto> innings
) {}
