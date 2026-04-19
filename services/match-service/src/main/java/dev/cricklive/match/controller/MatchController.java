package dev.cricklive.match.controller;

import dev.cricklive.match.domain.enums.MatchStatus;
import dev.cricklive.match.dto.*;
import dev.cricklive.match.service.MatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/matches")
@RequiredArgsConstructor
@Tag(name = "Matches", description = "Live match state and scorecard endpoints")
public class MatchController {

    private final MatchService matchService;

    @GetMapping("/live")
    @Operation(summary = "Get all LIVE matches", description = "Returns a list of all currently in-progress matches")
    @ApiResponse(responseCode = "200", description = "List of live matches (may be empty)")
    public ResponseEntity<List<MatchSummaryDto>> getLiveMatches() {
        return ResponseEntity.ok(matchService.getLiveMatches());
    }

    @GetMapping("/upcoming")
    @Operation(summary = "Get upcoming matches (paginated)")
    public ResponseEntity<Page<MatchSummaryDto>> getUpcoming(
            @PageableDefault(size = 20, sort = "scheduledStart") Pageable pageable) {
        return ResponseEntity.ok(matchService.getUpcomingMatches(pageable));
    }

    @GetMapping("/series/{seriesId}")
    @Operation(summary = "Get all matches for a series")
    public ResponseEntity<Page<MatchSummaryDto>> getBySeriesId(
            @PathVariable UUID seriesId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(matchService.getMatchesBySeries(seriesId, pageable));
    }

    @GetMapping("/{matchId}")
    @Operation(summary = "Get full match detail with scorecards",
               description = "Includes all innings, batting/bowling scorecards, toss, and result")
    @ApiResponse(responseCode = "200", description = "Match detail")
    @ApiResponse(responseCode = "404", description = "Match not found")
    public ResponseEntity<MatchDetailDto> getMatchDetail(
            @Parameter(description = "Match UUID") @PathVariable UUID matchId) {
        return ResponseEntity.ok(matchService.getMatchDetail(matchId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new match fixture (admin only)")
    @ApiResponse(responseCode = "201", description = "Match created")
    public ResponseEntity<MatchDetailDto> createMatch(@Valid @RequestBody CreateMatchRequest request) {
        MatchDetailDto created = matchService.createMatch(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PatchMapping("/{matchId}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SCORER')")
    @Operation(summary = "Update match status (admin / scorer)")
    public ResponseEntity<MatchSummaryDto> updateStatus(
            @PathVariable UUID matchId,
            @RequestParam MatchStatus status) {
        return ResponseEntity.ok(matchService.updateMatchStatus(matchId, status));
    }
}
