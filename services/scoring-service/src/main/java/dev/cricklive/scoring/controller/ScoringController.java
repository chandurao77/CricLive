package dev.cricklive.scoring.controller;

import dev.cricklive.scoring.dto.BallInputDto;
import dev.cricklive.scoring.service.ScoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/score")
@RequiredArgsConstructor
@Tag(name = "Scoring", description = "Ball-by-ball scoring ingestion (scorer admin only)")
public class ScoringController {

    private final ScoringService scoringService;

    @PostMapping("/ball")
    @PreAuthorize("hasRole('SCORER') or hasRole('ADMIN')")
    @Operation(
        summary = "Record a single ball delivery",
        description = "Validates, persists to event store, and publishes to Kafka. Idempotent — safe to retry with same idempotencyKey."
    )
    @ApiResponse(responseCode = "202", description = "Ball accepted and queued for processing")
    @ApiResponse(responseCode = "400", description = "Validation error in ball input")
    @ApiResponse(responseCode = "403", description = "Not a scorer or admin")
    public ResponseEntity<Void> recordBall(
            @Valid @RequestBody BallInputDto input,
            @AuthenticationPrincipal Jwt jwt) {

        UUID scorerId = UUID.fromString(jwt.getSubject());
        scoringService.recordBall(input, scorerId);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/undo")
    @PreAuthorize("hasRole('SCORER') or hasRole('ADMIN')")
    @Operation(
        summary = "Undo the last ball (correction)",
        description = "Appends a compensating UNDO event. Does not delete the original event."
    )
    @ApiResponse(responseCode = "202", description = "Undo accepted")
    public ResponseEntity<Void> undoLastBall(
            @RequestParam UUID inningsId,
            @AuthenticationPrincipal Jwt jwt) {

        // TODO: implement undo via compensating event
        return ResponseEntity.accepted().build();
    }
}
