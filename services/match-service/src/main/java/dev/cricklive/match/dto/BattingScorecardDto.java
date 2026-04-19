package dev.cricklive.match.dto;

import dev.cricklive.match.domain.enums.DismissalType;

import java.util.UUID;

public record BattingScorecardDto(
        UUID playerId,
        String playerName,
        short position,
        int runs,
        int ballsFaced,
        short fours,
        short sixes,
        double strikeRate,
        DismissalType dismissalType,
        String dismissalDescription,
        boolean didNotBat
) {}
