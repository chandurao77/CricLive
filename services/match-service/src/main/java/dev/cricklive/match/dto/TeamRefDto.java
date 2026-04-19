package dev.cricklive.match.dto;

import java.util.UUID;

public record TeamRefDto(UUID id, String name, String shortName, String flagUrl) {}
