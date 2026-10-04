package dev.cricklive.match.scoring;

import dev.cricklive.match.domain.entity.Innings;
import dev.cricklive.match.domain.entity.Match;
import dev.cricklive.match.domain.entity.Match.ResultType;
import dev.cricklive.match.domain.entity.Match.WinMarginUnit;
import dev.cricklive.match.domain.enums.MatchStatus;
import org.springframework.stereotype.Component;

/** Decides the result of a completed limited-overs match from its two innings. */
@Component
public class MatchResultCalculator {

    private static final int WICKETS_PER_INNINGS = 10;

    /** Sets result fields on the match and marks it COMPLETED. {@code first} batted first. */
    public void finish(Match match, Innings first, Innings second) {
        int chaseTotal = second.getTotalRuns();
        int toWin = first.getTotalRuns() + 1;

        if (chaseTotal >= toWin) {
            match.setResultType(ResultType.WIN);
            match.setWinningTeam(second.getBattingTeam());
            match.setWinMargin(WICKETS_PER_INNINGS - second.getWickets());
            match.setWinMarginUnit(WinMarginUnit.WICKETS);
        } else if (chaseTotal == first.getTotalRuns()) {
            match.setResultType(ResultType.TIE);
        } else {
            match.setResultType(ResultType.WIN);
            match.setWinningTeam(first.getBattingTeam());
            match.setWinMargin(first.getTotalRuns() - chaseTotal);
            match.setWinMarginUnit(WinMarginUnit.RUNS);
        }
        match.transitionTo(MatchStatus.COMPLETED);
    }
}
