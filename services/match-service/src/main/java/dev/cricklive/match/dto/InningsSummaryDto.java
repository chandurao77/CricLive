package dev.cricklive.match.dto;

import java.util.UUID;

public record InningsSummaryDto(
        UUID id,
        short inningsNumber,
        TeamRefDto battingTeam,
        int totalRuns,
        short wickets,
        String overs,
        double runRate
) {}
