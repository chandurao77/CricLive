package dev.cricklive.match.dto;

import dev.cricklive.match.domain.enums.InningsStatus;

import java.util.List;
import java.util.UUID;

public record InningsDetailDto(
        UUID id,
        short inningsNumber,
        TeamRefDto battingTeam,
        TeamRefDto bowlingTeam,
        int totalRuns,
        short wickets,
        String overs,
        ExtrasDto extras,
        InningsStatus status,
        Double requiredRunRate,
        Integer target,
        List<BattingScorecardDto> batting,
        List<BowlingScorecardDto> bowling
) {}
