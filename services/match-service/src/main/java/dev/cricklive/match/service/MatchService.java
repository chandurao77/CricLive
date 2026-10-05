package dev.cricklive.match.service;

import dev.cricklive.match.domain.enums.MatchStatus;
import dev.cricklive.match.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Contract for match lifecycle and scorecard retrieval operations.
 */
public interface MatchService {

    /**
     * Returns all currently LIVE matches, ordered by start time.
     */
    List<MatchSummaryDto> getLiveMatches();

    /**
     * Returns upcoming matches with pagination.
     */
    Page<MatchSummaryDto> getUpcomingMatches(Pageable pageable);

    /**
     * Returns all matches in a series.
     */
    Page<MatchSummaryDto> getMatchesBySeries(UUID seriesId, Pageable pageable);

    /**
     * Full match detail including all innings scorecards.
     *
     * @throws dev.cricklive.match.exception.MatchNotFoundException when id is unknown
     */
    MatchDetailDto getMatchDetail(UUID matchId);

    /**
     * Create a new match fixture.
     */
    MatchDetailDto createMatch(CreateMatchRequest request);

    /**
     * Transition match to a new status (admin / scoring-service driven).
     */
    MatchSummaryDto updateMatchStatus(UUID matchId, MatchStatus newStatus);

    /**
     * Returns completed matches, most recent first.
     */
    Page<MatchSummaryDto> getCompletedMatches(Pageable pageable);

    /**
     * Starts the next innings: creates it, sets the chase target when applicable, and puts the
     * match LIVE.
     *
     * @throws IllegalStateException when the match state does not allow a new innings
     */
    MatchDetailDto startInnings(UUID matchId, UUID battingTeamId);

    /**
     * Squads (home first, then away) for the match, used by the scoring console to pick batters and bowlers.
     *
     * @throws dev.cricklive.match.exception.MatchNotFoundException when id is unknown
     */
    List<SquadDto> getSquads(UUID matchId);
}
