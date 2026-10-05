package dev.cricklive.scoring.controller;

import dev.cricklive.scoring.dto.BallAck;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
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
        description = "Validates, persists to the event store, and publishes to Kafka. "
                + "Idempotent: safe to retry with the same idempotencyKey.")
    @ApiResponse(responseCode = "202", description = "Ball accepted and published")
    @ApiResponse(responseCode = "400", description = "Validation error in ball input")
    @ApiResponse(responseCode = "403", description = "Not a scorer or admin")
    @ApiResponse(responseCode = "503", description = "Event bus unavailable; ball not recorded, retry")
    public ResponseEntity<BallAck> recordBall(
            @Valid @RequestBody BallInputDto input,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.accepted().body(scoringService.recordBall(input, scorerIdOf(jwt)));
    }

    private static UUID scorerIdOf(Jwt jwt) {
        String subject = String.valueOf(jwt.getSubject());
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException ex) {
            return UUID.nameUUIDFromBytes(subject.getBytes(StandardCharsets.UTF_8));
        }
    }
}
