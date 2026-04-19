package dev.cricklive.match.dto;

import dev.cricklive.match.domain.enums.TossDecision;

import java.util.UUID;

public record TossDto(UUID winnerId, String winnerName, TossDecision decision) {}
