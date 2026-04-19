package dev.cricklive.scoring.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.util.UUID;

@Schema(description = "Input payload from the scorer for a single delivery")
public record BallInputDto(

        @NotNull
        @Schema(description = "UUID of the match") UUID matchId,

        @NotNull
        @Schema(description = "UUID of the innings") UUID inningsId,

        @NotNull
        @Schema(description = "UUID of the batter on strike") UUID batterId,

        @NotNull
        @Schema(description = "UUID of the bowler") UUID bowlerId,

        @Min(0) @Max(6)
        @Schema(description = "Runs scored by the batter (0-6)") int runsScored,

        @Schema(description = "True if wide delivery") boolean wide,

        @Schema(description = "True if no-ball") boolean noBall,

        @Schema(description = "True if byes awarded") boolean bye,

        @Schema(description = "True if leg-byes awarded") boolean legBye,

        @Min(0) @Max(5)
        @Schema(description = "Extra runs (wide penalty, bye, leg-bye)") int extraRuns,

        @Schema(description = "True if wicket fell on this delivery") boolean wicket,

        @Schema(description = "Dismissal type (required if wicket=true)") String dismissalType,

        @Schema(description = "UUID of dismissed batter (for run-outs of non-striker)") UUID dismissedBatterId,

        @Schema(description = "UUID of fielder involved in dismissal") UUID fielderId,

        @Schema(description = "Idempotency key — resend safe") String idempotencyKey
) {}
