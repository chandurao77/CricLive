package dev.cricklive.match.mapper;

import dev.cricklive.match.domain.entity.*;
import dev.cricklive.match.dto.*;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface MatchMapper {

    @Mapping(target = "seriesName", source = "series.name")
    @Mapping(target = "homeTeam", source = "homeTeam")
    @Mapping(target = "awayTeam", source = "awayTeam")
    @Mapping(target = "venueName", source = "venue.name")
    @Mapping(target = "venueCity", source = "venue.city")
    @Mapping(target = "statusText", expression = "java(buildStatusText(match))")
    @Mapping(target = "firstInnings", expression = "java(firstInnings(match))")
    @Mapping(target = "secondInnings", expression = "java(secondInnings(match))")
    MatchSummaryDto toSummaryDto(Match match);

    @Mapping(target = "seriesName", source = "series.name")
    @Mapping(target = "statusText", expression = "java(buildStatusText(match))")
    @Mapping(target = "venue", source = "venue")
    @Mapping(target = "toss", expression = "java(toTossDto(match))")
    @Mapping(target = "result", expression = "java(toResultDto(match))")
    @Mapping(target = "innings", source = "innings")
    MatchDetailDto toDetailDto(Match match, @Context Map<UUID, String> names);

    TeamRefDto toTeamRef(Team team);

    VenueDto toVenueDto(Venue venue);

    List<InningsDetailDto> toInningsDetailList(List<Innings> innings, @Context Map<UUID, String> names);

    @Mapping(target = "overs", expression = "java(formatOvers(innings.getOversCompleted()))")
    @Mapping(target = "extras", expression = "java(toExtrasDto(innings))")
    @Mapping(target = "batting", source = "battingEntries")
    @Mapping(target = "bowling", source = "bowlingEntries")
    @Mapping(target = "runRate", expression = "java(innings.currentRunRate())")
    @Mapping(target = "requiredRunRate",
             expression = "java(innings.requiredRunRate(innings.getMatch().getFormat().maxOvers()))")
    InningsDetailDto toInningsDetail(Innings innings, @Context Map<UUID, String> names);

    @Mapping(target = "overs", expression = "java(formatOvers(innings.getOversCompleted()))")
    @Mapping(target = "runRate", expression = "java(innings.currentRunRate())")
    InningsSummaryDto toInningsSummary(Innings innings);

    List<BattingScorecardDto> toBattingDtoList(List<BattingScorecard> entries, @Context Map<UUID, String> names);

    List<BowlingScorecardDto> toBowlingDtoList(List<BowlingScorecard> entries, @Context Map<UUID, String> names);

    @Mapping(target = "strikeRate", expression = "java(entry.strikeRate())")
    @Mapping(target = "playerName", expression = "java(nameOf(names, entry.getPlayerId()))")
    @Mapping(target = "dismissalDescription", expression = "java(describeDismissal(entry, names))")
    BattingScorecardDto toBattingDto(BattingScorecard entry, @Context Map<UUID, String> names);

    @Mapping(target = "overs", expression = "java(entry.getOvers().toPlainString())")
    @Mapping(target = "economy", expression = "java(entry.economy())")
    @Mapping(target = "playerName", expression = "java(nameOf(names, entry.getPlayerId()))")
    BowlingScorecardDto toBowlingDto(BowlingScorecard entry, @Context Map<UUID, String> names);

    default String formatOvers(BigDecimal overs) {
        return overs == null ? "0.0" : overs.toPlainString();
    }

    default String nameOf(Map<UUID, String> names, UUID playerId) {
        return playerId == null ? null : names.getOrDefault(playerId, "Unknown player");
    }

    default String describeDismissal(BattingScorecard entry, Map<UUID, String> names) {
        if (entry.getDismissalType() == null) {
            return "not out";
        }
        String bowler = nameOf(names, entry.getDismissedByBowlerId());
        String fielder = nameOf(names, entry.getDismissedByFielderId());
        return switch (entry.getDismissalType()) {
            case BOWLED -> "b " + bowler;
            case LBW -> "lbw b " + bowler;
            case CAUGHT -> fielder != null && !fielder.equals(bowler)
                    ? "c " + fielder + " b " + bowler
                    : "c & b " + bowler;
            case STUMPED -> "st " + (fielder != null ? fielder : "keeper") + " b " + bowler;
            case RUN_OUT -> "run out" + (fielder != null ? " (" + fielder + ")" : "");
            case HIT_WICKET -> "hit wicket b " + bowler;
            default -> entry.getDismissalType().name().toLowerCase().replace('_', ' ');
        };
    }

    default String buildStatusText(Match match) {
        return switch (match.getStatus()) {
            case LIVE -> liveText(match);
            case UPCOMING -> "Upcoming";
            case COMPLETED -> resultText(match);
            case ABANDONED -> "Abandoned";
            case RAIN_DELAY -> "Rain delay";
            case INNINGS_BREAK -> "Innings break";
            case TOSS -> "Toss";
            case NO_RESULT -> "No result";
        };
    }

    default String liveText(Match match) {
        Innings current = match.currentInnings();
        if (current == null || current.getTarget() == null || !match.getFormat().isLimitedOvers()) {
            return "Live";
        }
        int needed = current.getTarget() - current.getTotalRuns();
        int ballsLeft = match.getFormat().maxOvers() * 6 - current.legalBalls();
        return current.getBattingTeam().getShortName() + " need " + Math.max(needed, 0)
                + " runs from " + Math.max(ballsLeft, 0) + " balls";
    }

    default String resultText(Match match) {
        if (match.getResultType() == null) {
            return "Completed";
        }
        return switch (match.getResultType()) {
            case WIN -> match.getWinningTeam().getName() + " won by " + match.getWinMargin() + " "
                    + singularize(match.getWinMarginUnit().name().toLowerCase(), match.getWinMargin());
            case TIE -> "Match tied";
            case DRAW -> "Match drawn";
            case NO_RESULT -> "No result";
            case ABANDONED -> "Abandoned";
        };
    }

    default String singularize(String unit, int amount) {
        return amount == 1 ? unit.substring(0, unit.length() - 1) : unit;
    }

    default InningsSummaryDto firstInnings(Match match) {
        if (match.getInnings().isEmpty()) return null;
        return toInningsSummary(match.getInnings().get(0));
    }

    default InningsSummaryDto secondInnings(Match match) {
        if (match.getInnings().size() < 2) return null;
        return toInningsSummary(match.getInnings().get(1));
    }

    default TossDto toTossDto(Match match) {
        if (match.getTossWinner() == null) return null;
        return new TossDto(match.getTossWinner().getId(), match.getTossWinner().getName(), match.getTossDecision());
    }

    default ResultDto toResultDto(Match match) {
        if (match.getResultType() == null) return null;
        String winnerName = match.getWinningTeam() != null ? match.getWinningTeam().getName() : null;
        UUID winnerId = match.getWinningTeam() != null ? match.getWinningTeam().getId() : null;
        return new ResultDto(match.getResultType(), winnerId, winnerName, match.getWinMargin(), match.getWinMarginUnit());
    }

    default ExtrasDto toExtrasDto(Innings innings) {
        return new ExtrasDto(
                innings.getExtrasTotal(), innings.getExtrasWides(), innings.getExtrasNoBalls(),
                innings.getExtrasByes(), innings.getExtrasLegByes(), innings.getExtrasPenalty()
        );
    }
}
