package dev.cricklive.match.service;

import dev.cricklive.match.domain.entity.Innings;
import dev.cricklive.match.domain.entity.Match;
import dev.cricklive.match.domain.enums.InningsStatus;
import dev.cricklive.match.domain.enums.MatchStatus;
import dev.cricklive.match.dto.BallEventMessage;
import dev.cricklive.match.repository.InningsRepository;
import dev.cricklive.match.scoring.InningsScorer;
import dev.cricklive.match.scoring.MatchResultCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies ball events to the persisted scorecard, closes innings, and decides limited-overs
 * results. One transaction per event; the Kafka key (matchId) guarantees per-match ordering.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScoreboardService {

    private final InningsRepository inningsRepository;
    private final InningsScorer scorer;
    private final MatchResultCalculator resultCalculator;
    private final ScorecardCacheService cacheService;

    /**
     * Updates the scorecard for one delivery and publishes the new state.
     *
     * @throws IllegalStateException if the innings does not exist (retried, then dropped, by the consumer)
     */
    @Transactional
    public void applyBallEvent(BallEventMessage event) {
        Innings innings = inningsRepository.findById(event.inningsId())
                .orElseThrow(() -> new IllegalStateException("Innings not found: " + event.inningsId()));
        Match match = innings.getMatch();

        if (!scorer.apply(innings, event)) {
            log.debug("Ignored ball event eventId={} seq={} (duplicate or innings not in progress)",
                    event.eventId(), event.sequence());
            return;
        }

        if (scorer.isOver(innings, match.getFormat().maxOvers())) {
            closeInnings(match, innings);
        }

        inningsRepository.save(innings);
        cacheService.publishScorecardUpdate(match, innings);
    }

    private void closeInnings(Match match, Innings innings) {
        innings.setStatus(InningsStatus.COMPLETED);
        if (!match.getFormat().isLimitedOvers()) {
            return;
        }
        if (innings.getInningsNumber() == 1) {
            match.transitionTo(MatchStatus.INNINGS_BREAK);
        } else {
            Innings first = match.getInnings().stream()
                    .filter(i -> i.getInningsNumber() == 1)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("First innings missing for match " + match.getId()));
            resultCalculator.finish(match, first, innings);
        }
        log.info("Innings {} of match {} completed; match status={}",
                innings.getInningsNumber(), match.getId(), match.getStatus());
    }
}
