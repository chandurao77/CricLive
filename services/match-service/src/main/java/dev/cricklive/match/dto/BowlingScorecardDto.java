package dev.cricklive.match.dto;

import java.util.UUID;

public record BowlingScorecardDto(
        UUID playerId,
        String playerName,
        String overs,
        short maidens,
        int runs,
        short wickets,
        short wides,
        short noBalls,
        double economy
) {}
