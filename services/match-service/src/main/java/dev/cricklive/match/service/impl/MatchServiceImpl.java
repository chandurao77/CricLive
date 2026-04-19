package dev.cricklive.match.service.impl;

import dev.cricklive.match.domain.entity.*;
import dev.cricklive.match.domain.enums.InningsStatus;
import dev.cricklive.match.domain.enums.MatchStatus;
import dev.cricklive.match.dto.*;
import dev.cricklive.match.exception.MatchNotFoundException;
import dev.cricklive.match.mapper.MatchMapper;
import dev.cricklive.match.repository.*;
import dev.cricklive.match.service.MatchService;
import dev.cricklive.match.service.ScorecardCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MatchServiceImpl implements MatchService {

    private final MatchRepository matchRepository;
    private final InningsRepository inningsRepository;
    private final TeamRepository teamRepository;
    private final MatchMapper matchMapper;
    private final ScorecardCacheService cacheService;

    @Override
    public List<MatchSummaryDto> getLiveMatches() {
        return matchRepository.findByStatusOrderByScheduledStartAsc(MatchStatus.LIVE)
                .stream()
                .map(matchMapper::toSummaryDto)
                .toList();
    }

    @Override
    public Page<MatchSummaryDto> getUpcomingMatches(Pageable pageable) {
        return matchRepository.findAll(pageable).map(matchMapper::toSummaryDto);
    }

    @Override
    public Page<MatchSummaryDto> getMatchesBySeries(UUID seriesId, Pageable pageable) {
        return matchRepository.findBySeriesId(seriesId, pageable).map(matchMapper::toSummaryDto);
    }

    @Override
    public MatchDetailDto getMatchDetail(UUID matchId) {
        Match match = matchRepository.findByIdWithDetails(matchId)
                .orElseThrow(() -> new MatchNotFoundException(matchId));
        return matchMapper.toDetailDto(match);
    }

    @Override
    @Transactional
    public MatchDetailDto createMatch(CreateMatchRequest request) {
        Team home = teamRepository.findById(request.homeTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Home team not found: " + request.homeTeamId()));
        Team away = teamRepository.findById(request.awayTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Away team not found: " + request.awayTeamId()));

        Match match = Match.builder()
                .format(request.format())
                .scheduledStart(request.scheduledStart())
                .homeTeam(home)
                .awayTeam(away)
                .matchNumber(request.matchNumber())
                .status(MatchStatus.UPCOMING)
                .build();

        Match saved = matchRepository.save(match);
        log.info("Created match id={} format={} home={} away={}", saved.getId(), saved.getFormat(), home.getShortName(), away.getShortName());
        return matchMapper.toDetailDto(saved);
    }

    @Override
    @Transactional
    public MatchSummaryDto updateMatchStatus(UUID matchId, MatchStatus newStatus) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchNotFoundException(matchId));
        match.transitionTo(newStatus);
        Match saved = matchRepository.save(match);
        log.info("Match {} transitioned to {}", matchId, newStatus);
        return matchMapper.toSummaryDto(saved);
    }

    @Override
    @Transactional
    @KafkaListener(topics = "${cricklive.kafka.topics.ball-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void applyBallEvent(BallEventMessage event) {
        log.debug("Applying ball event matchId={} over={}.{}", event.matchId(), event.overNumber(), event.ballNumber());

        Innings innings = inningsRepository.findById(event.inningsId())
                .orElseThrow(() -> new IllegalStateException("Innings not found: " + event.inningsId()));

        int deliveryRuns = event.runsScored() + event.extraRuns();
        innings.setTotalRuns(innings.getTotalRuns() + deliveryRuns);

        if (!event.isWide() && !event.isNoBall()) {
            updateOvers(innings, event.overNumber(), event.ballNumber());
        }
        if (event.isWide()) innings.setExtrasWides(innings.getExtrasWides() + 1 + event.extraRuns());
        if (event.isNoBall()) innings.setExtrasNoBalls(innings.getExtrasNoBalls() + 1 + event.extraRuns());
        if (event.isBye()) innings.setExtrasByes(innings.getExtrasByes() + event.extraRuns());
        if (event.isLegBye()) innings.setExtrasLegByes(innings.getExtrasLegByes() + event.extraRuns());

        innings.setExtrasTotal(
                innings.getExtrasWides() + innings.getExtrasNoBalls() +
                innings.getExtrasByes() + innings.getExtrasLegByes() + innings.getExtrasPenalty()
        );

        if (event.isWicket()) {
            innings.setWickets((short) (innings.getWickets() + 1));
        }

        inningsRepository.save(innings);
        cacheService.publishScorecardUpdate(innings.getMatch().getId(), innings);
    }

    private void updateOvers(Innings innings, int overNum, int ballNum) {
        double currentOvers = overNum + (ballNum / 10.0);
        innings.setOversCompleted(java.math.BigDecimal.valueOf(currentOvers));
    }
}
