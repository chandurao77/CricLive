package dev.cricklive.commentary.controller;

import dev.cricklive.commentary.domain.CommentaryDocument;
import dev.cricklive.commentary.domain.CommentaryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/commentary")
@RequiredArgsConstructor
@Tag(name = "Commentary", description = "Ball-by-ball commentary retrieval (REST + WebSocket)")
public class CommentaryController {

    private final CommentaryRepository repository;

    @GetMapping("/{matchId}/latest")
    @Operation(summary = "Get last N commentary entries for a match",
               description = "Used on reconnect to catch up missed events. Default 20 entries.")
    public ResponseEntity<List<CommentaryDocument>> getLatest(
            @PathVariable UUID matchId,
            @RequestParam(defaultValue = "20") int limit) {
        List<CommentaryDocument> result =
                repository.findByMatchIdOrderByTimestampDesc(matchId, PageRequest.of(0, limit));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{matchId}/innings/{inningsId}")
    @Operation(summary = "Get full commentary for an innings in chronological order")
    public ResponseEntity<List<CommentaryDocument>> getInningsCommentary(
            @PathVariable UUID matchId,
            @PathVariable UUID inningsId) {
        return ResponseEntity.ok(
                repository.findByMatchIdAndInningsIdOrderByOverNumberAscBallNumberAsc(matchId, inningsId));
    }

    @GetMapping("/{matchId}/innings/{inningsId}/over/{over}")
    @Operation(summary = "Get commentary for a specific over")
    public ResponseEntity<List<CommentaryDocument>> getOverCommentary(
            @PathVariable UUID matchId,
            @PathVariable UUID inningsId,
            @PathVariable int over) {
        return ResponseEntity.ok(
                repository.findByMatchIdAndInningsIdAndOverNumberOrderByBallNumberAsc(matchId, inningsId, over));
    }
}
