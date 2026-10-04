package dev.cricklive.match.scoring;

import dev.cricklive.match.domain.entity.BattingScorecard;
import dev.cricklive.match.domain.entity.BowlingScorecard;
import dev.cricklive.match.domain.entity.Innings;
import dev.cricklive.match.domain.enums.DismissalType;
import dev.cricklive.match.domain.enums.InningsStatus;
import dev.cricklive.match.dto.BallEventMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InningsScorerTest {

    private static final UUID BATTER = UUID.randomUUID();
    private static final UUID BATTER_2 = UUID.randomUUID();
    private static final UUID BOWLER = UUID.randomUUID();
    private static final UUID FIELDER = UUID.randomUUID();

    private final InningsScorer scorer = new InningsScorer();
    private Innings innings;
    private long seq;
    private int legal;

    @BeforeEach
    void setUp() {
        innings = Innings.builder().status(InningsStatus.IN_PROGRESS).build();
        seq = 0;
        legal = 0;
    }

    /** Builds the next event in sequence; legalBall is derived from wide/noBall like the scoring-service does. */
    private BallEventMessage next(int runs, boolean wide, boolean noBall, boolean bye, boolean legBye, int extras,
                                  boolean wicket, String dismissal, UUID batter) {
        boolean isLegal = !wide && !noBall;
        if (isLegal) legal++;
        return new BallEventMessage("e" + (++seq), seq, UUID.randomUUID(), UUID.randomUUID(), 0, 0, isLegal, legal,
                batter, BOWLER, runs, wicket, dismissal, null, FIELDER, wide, noBall, bye, legBye, extras,
                runs == 4 || runs == 6, runs == 6, Instant.now(), UUID.randomUUID());
    }

    private BallEventMessage runs(int runs) {
        return next(runs, false, false, false, false, 0, false, null, BATTER);
    }

    private BattingScorecard batter(UUID id) {
        return innings.getBattingEntries().stream().filter(b -> b.getPlayerId().equals(id)).findFirst().orElseThrow();
    }

    private BowlingScorecard bowler() {
        return innings.getBowlingEntries().stream().filter(b -> b.getPlayerId().equals(BOWLER)).findFirst().orElseThrow();
    }

    @Test
    void apply_runsOffBat_creditsBatterBowlerAndTotal() {
        scorer.apply(innings, runs(4));
        scorer.apply(innings, runs(6));
        scorer.apply(innings, runs(1));

        assertThat(innings.getTotalRuns()).isEqualTo(11);
        assertThat(batter(BATTER).getRuns()).isEqualTo(11);
        assertThat(batter(BATTER).getBallsFaced()).isEqualTo(3);
        assertThat(batter(BATTER).getFours()).isEqualTo((short) 1);
        assertThat(batter(BATTER).getSixes()).isEqualTo((short) 1);
        assertThat(bowler().getRuns()).isEqualTo(11);
        assertThat(bowler().getOvers()).isEqualByComparingTo("0.3");
        assertThat(innings.getOversCompleted()).isEqualByComparingTo("0.3");
    }

    @Test
    void apply_sixthBall_rollsOverToNextOver() {
        for (int i = 0; i < 6; i++) scorer.apply(innings, runs(0));
        assertThat(innings.getOversCompleted()).isEqualByComparingTo("1.0");
        scorer.apply(innings, runs(0));
        assertThat(innings.getOversCompleted()).isEqualByComparingTo("1.1");
        assertThat(bowler().getOvers()).isEqualByComparingTo("1.1");
    }

    @Test
    void apply_overWithNoRunsOffBowler_isAMaiden() {
        for (int i = 0; i < 5; i++) scorer.apply(innings, runs(0));
        scorer.apply(innings, next(0, false, false, true, false, 1, false, null, BATTER)); // bye: not charged to bowler
        assertThat(bowler().getMaidens()).isEqualTo((short) 1);
    }

    @Test
    void apply_overWithAWide_isNotAMaiden() {
        for (int i = 0; i < 5; i++) scorer.apply(innings, runs(0));
        scorer.apply(innings, next(0, true, false, false, false, 1, false, null, BATTER));
        scorer.apply(innings, runs(0));
        assertThat(bowler().getMaidens()).isZero();
        assertThat(bowler().getCurrentOverRuns()).isZero();
    }

    @Test
    void apply_wide_addsExtrasChargesBowlerButNoBallFaced() {
        scorer.apply(innings, next(0, true, false, false, false, 1, false, null, BATTER));

        assertThat(innings.getTotalRuns()).isEqualTo(1);
        assertThat(innings.getExtrasWides()).isEqualTo(1);
        assertThat(innings.getExtrasTotal()).isEqualTo(1);
        assertThat(innings.getOversCompleted()).isEqualByComparingTo("0.0");
        assertThat(batter(BATTER).getBallsFaced()).isZero();
        assertThat(bowler().getRuns()).isEqualTo(1);
        assertThat(bowler().getWides()).isEqualTo((short) 1);
        assertThat(bowler().getOvers()).isEqualByComparingTo("0.0");
    }

    @Test
    void apply_wideThatRanFour_countsAllAsWides() {
        scorer.apply(innings, next(0, true, false, false, false, 5, false, null, BATTER));

        assertThat(innings.getTotalRuns()).isEqualTo(5);
        assertThat(innings.getExtrasWides()).isEqualTo(5);
        assertThat(bowler().getRuns()).isEqualTo(5);
    }

    @Test
    void apply_noBallWithFour_batterGetsRunsBowlerConcedesFiveBallFacedNotLegal() {
        scorer.apply(innings, next(4, false, true, false, false, 1, false, null, BATTER));

        assertThat(innings.getTotalRuns()).isEqualTo(5);
        assertThat(innings.getExtrasNoBalls()).isEqualTo(1);
        assertThat(innings.getOversCompleted()).isEqualByComparingTo("0.0");
        assertThat(batter(BATTER).getRuns()).isEqualTo(4);
        assertThat(batter(BATTER).getBallsFaced()).isEqualTo(1);
        assertThat(bowler().getRuns()).isEqualTo(5);
        assertThat(bowler().getNoBalls()).isEqualTo((short) 1);
    }

    @Test
    void apply_byes_addToTotalAndByesButNotBowlerOrBatter() {
        scorer.apply(innings, next(0, false, false, true, false, 2, false, null, BATTER));

        assertThat(innings.getTotalRuns()).isEqualTo(2);
        assertThat(innings.getExtrasByes()).isEqualTo(2);
        assertThat(batter(BATTER).getRuns()).isZero();
        assertThat(batter(BATTER).getBallsFaced()).isEqualTo(1);
        assertThat(bowler().getRuns()).isZero();
        assertThat(innings.getOversCompleted()).isEqualByComparingTo("0.1");
    }

    @Test
    void apply_legByesOnNoBall_firstRunIsNoBallPenalty() {
        scorer.apply(innings, next(0, false, true, false, true, 3, false, null, BATTER));

        assertThat(innings.getTotalRuns()).isEqualTo(3);
        assertThat(innings.getExtrasNoBalls()).isEqualTo(1);
        assertThat(innings.getExtrasLegByes()).isEqualTo(2);
        assertThat(innings.getExtrasTotal()).isEqualTo(3);
    }

    @Test
    void apply_bowledWicket_creditsBowlerAndRecordsDismissal() {
        scorer.apply(innings, next(0, false, false, false, false, 0, true, "BOWLED", BATTER));

        assertThat(innings.getWickets()).isEqualTo((short) 1);
        assertThat(batter(BATTER).getDismissalType()).isEqualTo(DismissalType.BOWLED);
        assertThat(batter(BATTER).getDismissedByBowlerId()).isEqualTo(BOWLER);
        assertThat(bowler().getWickets()).isEqualTo((short) 1);
    }

    @Test
    void apply_runOut_notCreditedToBowlerAndUsesDismissedBatter() {
        BallEventMessage e = next(1, false, false, false, false, 0, true, "RUN_OUT", BATTER);
        BallEventMessage runOut = new BallEventMessage(e.eventId(), e.sequence(), e.matchId(), e.inningsId(), 0, 0,
                true, e.legalBallsInInnings(), BATTER, BOWLER, 1, true, "RUN_OUT", BATTER_2, FIELDER,
                false, false, false, false, 0, false, false, Instant.now(), UUID.randomUUID());
        scorer.apply(innings, runOut);

        assertThat(innings.getWickets()).isEqualTo((short) 1);
        assertThat(batter(BATTER_2).getDismissalType()).isEqualTo(DismissalType.RUN_OUT);
        assertThat(batter(BATTER_2).getDismissedByBowlerId()).isNull();
        assertThat(batter(BATTER_2).getDismissedByFielderId()).isEqualTo(FIELDER);
        assertThat(bowler().getWickets()).isZero();
        assertThat(batter(BATTER).getRuns()).isEqualTo(1);
    }

    @Test
    void apply_duplicateOrOlderSequence_ignored() {
        BallEventMessage first = runs(4);
        assertThat(scorer.apply(innings, first)).isTrue();
        assertThat(scorer.apply(innings, first)).isFalse();
        assertThat(innings.getTotalRuns()).isEqualTo(4);
        assertThat(innings.getLastEventSeq()).isEqualTo(1);
    }

    @Test
    void apply_sequenceGap_stillApplied() {
        scorer.apply(innings, runs(1));
        seq += 5;
        assertThat(scorer.apply(innings, runs(2))).isTrue();
        assertThat(innings.getTotalRuns()).isEqualTo(3);
    }

    @Test
    void apply_inningsNotInProgress_ignored() {
        innings.setStatus(InningsStatus.COMPLETED);
        assertThat(scorer.apply(innings, runs(6))).isFalse();
        assertThat(innings.getTotalRuns()).isZero();
    }

    @Test
    void batters_getPositionsInOrderOfAppearance() {
        scorer.apply(innings, next(1, false, false, false, false, 0, false, null, BATTER));
        scorer.apply(innings, next(1, false, false, false, false, 0, false, null, BATTER_2));
        assertThat(batter(BATTER).getPosition()).isEqualTo((short) 1);
        assertThat(batter(BATTER_2).getPosition()).isEqualTo((short) 2);
    }

    @Test
    void isOver_allOut_true() {
        innings.setWickets((short) 10);
        assertThat(scorer.isOver(innings, 20)).isTrue();
    }

    @Test
    void isOver_oversDone_trueOnlyForLimitedOvers() {
        innings.setOversCompleted(new BigDecimal("20.0"));
        assertThat(scorer.isOver(innings, 20)).isTrue();
        assertThat(scorer.isOver(innings, 0)).isFalse();
    }

    @Test
    void isOver_targetReached_true() {
        innings.setTarget(150);
        innings.setTotalRuns(149);
        assertThat(scorer.isOver(innings, 20)).isFalse();
        innings.setTotalRuns(150);
        assertThat(scorer.isOver(innings, 20)).isTrue();
    }

    @Test
    void oversConversion_roundTrips() {
        for (int balls = 0; balls <= 300; balls++) {
            assertThat(InningsScorer.ballsFromOvers(InningsScorer.oversFromBalls(balls))).isEqualTo(balls);
        }
    }

    @Test
    void runRates_useBallsNotDecimalOvers() {
        innings.setStatus(InningsStatus.IN_PROGRESS);
        innings.setTotalRuns(12);
        innings.setOversCompleted(new BigDecimal("0.4"));
        assertThat(innings.currentRunRate()).isEqualTo(18.0);

        innings.setTarget(100);
        innings.setTotalRuns(40);
        innings.setOversCompleted(new BigDecimal("10.0"));
        assertThat(innings.requiredRunRate(20)).isEqualTo(6.0);
    }
}
