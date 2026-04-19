package dev.cricklive.match.dto;

import dev.cricklive.match.domain.entity.Match.ResultType;
import dev.cricklive.match.domain.entity.Match.WinMarginUnit;

import java.util.UUID;

public record ResultDto(ResultType type, UUID winningTeamId, String winningTeamName, Integer margin, WinMarginUnit marginUnit) {}
