package dev.cricklive.match.mapper;

import dev.cricklive.match.domain.entity.*;
import dev.cricklive.match.dto.*;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.util.List;

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
    @Mapping(target = "venue", source = "venue")
    @Mapping(target = "toss", expression = "java(toTossDto(match))")
    @Mapping(target = "result", expression = "java(toResultDto(match))")
    @Mapping(target = "innings", source = "innings")
    MatchDetailDto toDetailDto(Match match);

    TeamRefDto toTeamRef(Team team);

    VenueDto toVenueDto(Venue venue);

    @Mapping(target = "overs", expression = "java(formatOvers(innings.getOversCompleted()))")
    @Mapping(target = "extras", expression = "java(toExtrasDto(innings))")
    @Mapping(target = "batting", source = "battingEntries")
    @Mapping(target = "bowling", source = "bowlingEntries")
    @Mapping(target = "requiredRunRate", expression = "java(innings.requiredRunRate(50))")
    InningsDetailDto toInningsDetail(Innings innings);

    @Mapping(target = "overs", expression = "java(formatOvers(innings.getOversCompleted()))")
    @Mapping(target = "runRate", expression = "java(innings.currentRunRate())")
    InningsSummaryDto toInningsSummary(Innings innings);

    @Mapping(target = "strikeRate", expression = "java(entry.strikeRate())")
    @Mapping(target = "playerName", ignore = true)
    @Mapping(target = "dismissalDescription", ignore = true)
    BattingScorecardDto toBattingDto(BattingScorecard entry);

    @Mapping(target = "overs", expression = "java(entry.getOvers().toPlainString())")
    @Mapping(target = "economy", expression = "java(entry.economy())")
    @Mapping(target = "playerName", ignore = true)
    BowlingScorecardDto toBowlingDto(BowlingScorecard entry);

    List<BattingScorecardDto> toBattingDtoList(List<BattingScorecard> entries);

    List<BowlingScorecardDto> toBowlingDtoList(List<BowlingScorecard> entries);

    default String formatOvers(BigDecimal overs) {
        if (overs == null) return "0.0";
        int completedOvers = overs.intValue();
        int balls = (int) Math.round((overs.doubleValue() - completedOvers) * 10);
        return completedOvers + "." + balls;
    }

    default String buildStatusText(Match match) {
        return switch (match.getStatus()) {
            case LIVE -> "Live";
            case UPCOMING -> "Upcoming";
            case COMPLETED -> "Completed";
            case ABANDONED -> "Abandoned";
            case RAIN_DELAY -> "Rain delay";
            case INNINGS_BREAK -> "Innings break";
            default -> match.getStatus().name();
        };
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
        java.util.UUID winnerId = match.getWinningTeam() != null ? match.getWinningTeam().getId() : null;
        return new ResultDto(match.getResultType(), winnerId, winnerName, match.getWinMargin(), match.getWinMarginUnit());
    }

    default ExtrasDto toExtrasDto(Innings innings) {
        return new ExtrasDto(
                innings.getExtrasTotal(), innings.getExtrasWides(), innings.getExtrasNoBalls(),
                innings.getExtrasByes(), innings.getExtrasLegByes(), innings.getExtrasPenalty()
        );
    }
}
