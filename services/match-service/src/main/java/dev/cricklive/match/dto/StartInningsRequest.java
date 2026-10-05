package dev.cricklive.match.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Payload to start the next innings of a match")
public record StartInningsRequest(
        @NotNull @Schema(description = "Team that bats in this innings (must be home or away team)") UUID battingTeamId
) {}
