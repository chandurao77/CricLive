package dev.cricklive.match.service.impl;

import dev.cricklive.match.domain.entity.*;
import dev.cricklive.match.domain.enums.InningsStatus;
import dev.cricklive.match.domain.enums.MatchStatus;
import dev.cricklive.match.dto.*;
import dev.cricklive.match.exception.MatchNotFoundException;
import dev.cricklive.match.mapper.MatchMapper;
import dev.cricklive.match.repository.MatchRepository;
import dev.cricklive.match.repository.PlayerRepository;
import dev.cricklive.match.repository.TeamRepository;
import dev.cricklive.match.service.MatchService;
import dev.cricklive.match.service.ScorecardCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MatchServiceImpl implements MatchService {

    private static final List<MatchStatus> IN_PROGRESS_STATUSES =
            List.of(MatchStatus.LIVE, MatchStatus.INNINGS_BREAK, MatchStatus.RAIN_DELAY);
    private static final List<MatchStatus> FINISHED_STATUSES =
            List.of(MatchStatus.COMPLETED, MatchStatus.ABANDONED, MatchStatus.NO_RESULT);

    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final MatchMapper matchMapper;
    private final ScorecardCacheService cacheService;

    @Override
    public List<MatchSummaryDto> getLiveMatches() {
        return matchRepository.findByStatusIn(IN_PROGRESS_STATUSES)
                .stream()
                .map(matchMapper::toSummaryDto)
                .toList();
    }

    @Override
    public Page<MatchSummaryDto> getUpcomingMatches(Pageable pageable) {
        return matchRepository.findByStatus(MatchStatus.UPCOMING, pageable).map(matchMapper::toSummaryDto);
    }

    @Override
    public Page<MatchSummaryDto> getCompletedMatches(Pageable pageable) {
        return matchRepository.findByStatusIn(FINISHED_STATUSES, pageable).map(matchMapper::toSummaryDto);
    }

    @Override
    public Page<MatchSummaryDto> getMatchesBySeries(UUID seriesId, Pageable pageable) {
        return matchRepository.findBySeriesId(seriesId, pageable).map(matchMapper::toSummaryDto);
    }

    @Override
    public MatchDetailDto getMatchDetail(UUID matchId) {
        Match match = matchRepository.findByIdWithDetails(matchId)
                .orElseThrow(() -> new MatchNotFoundException(matchId));
        return toDetail(match);
    }

    @Override
    public List<SquadDto> getSquads(UUID matchId) {
        Match match = matchRepository.findByIdWithDetails(matchId)
                .orElseThrow(() -> new MatchNotFoundException(matchId));
        return List.of(match.getHomeTeam(), match.getAwayTeam()).stream()
                .map(team -> new SquadDto(
                        matchMapper.toTeamRef(team),
                        playerRepository.findByTeamIdOrderByNameAsc(team.getId()).stream()
                                .map(p -> new PlayerDto(p.getId(), p.getName(), p.getRole()))
                                .toList()))
                .toList();
    }

    @Override
    @Transactional
    public MatchDetailDto createMatch(CreateMatchRequest request) {
        Team home = teamRepository.findById(request.homeTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Home team not found: " + request.homeTeamId()));
        Team away = teamRepository.findById(request.awayTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Away team not found: " + request.awayTeamId()));
        if (home.getId().equals(away.getId())) {
            throw new IllegalArgumentException("Home and away team must differ");
        }

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
        return toDetail(saved);
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
    public MatchDetailDto startInnings(UUID matchId, UUID battingTeamId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchNotFoundException(matchId));
        if (FINISHED_STATUSES.contains(match.getStatus())) {
            throw new IllegalStateException("Match is already " + match.getStatus());
        }

        List<Innings> existing = match.getInnings();
        if (existing.stream().anyMatch(i -> i.getStatus() == InningsStatus.IN_PROGRESS)) {
            throw new IllegalStateException("An innings is already in progress");
        }
        if (existing.size() >= match.getFormat().maxInnings()) {
            throw new IllegalStateException("All " + match.getFormat().maxInnings() + " innings have been played");
        }

        Team batting = teamOf(match, battingTeamId);
        Team bowling = batting.getId().equals(match.getHomeTeam().getId()) ? match.getAwayTeam() : match.getHomeTeam();
        boolean limited = match.getFormat().isLimitedOvers();
        if (limited && !existing.isEmpty() && existing.get(0).getBattingTeam().getId().equals(batting.getId())) {
            throw new IllegalArgumentException("The second innings must be batted by the other team");
        }

        int number = existing.size() + 1;
        Integer target = limited && number == 2 ? existing.get(0).getTotalRuns() + 1 : null;
        Innings innings = Innings.builder()
                .match(match)
                .inningsNumber((short) number)
                .battingTeam(batting)
                .bowlingTeam(bowling)
                .status(InningsStatus.IN_PROGRESS)
                .target(target)
                .build();
        existing.add(innings);
        match.transitionTo(MatchStatus.LIVE);

        Match saved = matchRepository.saveAndFlush(match);
        cacheService.publishScorecardUpdate(saved, innings);
        log.info("Started innings {} of match {} (batting={}, target={})", number, matchId, batting.getShortName(), target);
        return toDetail(saved);
    }

    private Team teamOf(Match match, UUID teamId) {
        if (match.getHomeTeam().getId().equals(teamId)) {
            return match.getHomeTeam();
        }
        if (match.getAwayTeam().getId().equals(teamId)) {
            return match.getAwayTeam();
        }
        throw new IllegalArgumentException("Batting team must be the home or away team of this match");
    }

    private MatchDetailDto toDetail(Match match) {
        return matchMapper.toDetailDto(match, playerNames(match));
    }

    private Map<UUID, String> playerNames(Match match) {
        Set<UUID> ids = new HashSet<>();
        for (Innings innings : match.getInnings()) {
            for (BattingScorecard b : innings.getBattingEntries()) {
                ids.add(b.getPlayerId());
                if (b.getDismissedByBowlerId() != null) ids.add(b.getDismissedByBowlerId());
                if (b.getDismissedByFielderId() != null) ids.add(b.getDismissedByFielderId());
            }
            innings.getBowlingEntries().forEach(b -> ids.add(b.getPlayerId()));
        }
        return playerRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Player::getId, Player::getName));
    }
}
