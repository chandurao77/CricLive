package dev.cricklive.match.scoring;

import dev.cricklive.match.domain.entity.Innings;
import dev.cricklive.match.domain.entity.Match;
import dev.cricklive.match.domain.entity.Match.ResultType;
import dev.cricklive.match.domain.entity.Match.WinMarginUnit;
import dev.cricklive.match.domain.entity.Team;
import dev.cricklive.match.domain.enums.MatchStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MatchResultCalculatorTest {

    private final MatchResultCalculator calculator = new MatchResultCalculator();
    private final Team first = Team.builder().name("India").shortName("IND").build();
    private final Team second = Team.builder().name("Australia").shortName("AUS").build();

    private Innings innings(Team team, int runs, int wickets) {
        return Innings.builder().battingTeam(team).totalRuns(runs).wickets((short) wickets).build();
    }

    @Test
    void finish_chaseSucceeds_secondTeamWinsByWicketsInHand() {
        Match match = Match.builder().build();
        calculator.finish(match, innings(first, 150, 6), innings(second, 151, 4));

        assertThat(match.getResultType()).isEqualTo(ResultType.WIN);
        assertThat(match.getWinningTeam()).isSameAs(second);
        assertThat(match.getWinMargin()).isEqualTo(6);
        assertThat(match.getWinMarginUnit()).isEqualTo(WinMarginUnit.WICKETS);
        assertThat(match.getStatus()).isEqualTo(MatchStatus.COMPLETED);
    }

    @Test
    void finish_chaseFallsShort_firstTeamWinsByRuns() {
        Match match = Match.builder().build();
        calculator.finish(match, innings(first, 180, 7), innings(second, 160, 10));

        assertThat(match.getWinningTeam()).isSameAs(first);
        assertThat(match.getWinMargin()).isEqualTo(20);
        assertThat(match.getWinMarginUnit()).isEqualTo(WinMarginUnit.RUNS);
    }

    @Test
    void finish_scoresLevel_isATie() {
        Match match = Match.builder().build();
        calculator.finish(match, innings(first, 170, 8), innings(second, 170, 9));

        assertThat(match.getResultType()).isEqualTo(ResultType.TIE);
        assertThat(match.getWinningTeam()).isNull();
        assertThat(match.getStatus()).isEqualTo(MatchStatus.COMPLETED);
    }
}
