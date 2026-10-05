package dev.cricklive.match.dto;

import java.util.List;

/** A team and its registered players. */
public record SquadDto(TeamRefDto team, List<PlayerDto> players) {}
