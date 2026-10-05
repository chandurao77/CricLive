package dev.cricklive.scoring.service;

import dev.cricklive.scoring.dto.BallInputDto;
import dev.cricklive.scoring.exception.BallRejectedException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BallRulesTest {

    private static final UUID ID = UUID.randomUUID();

    private static BallInputDto ball(int runs, boolean wide, boolean noBall, boolean bye, boolean legBye,
                                     int extras, boolean wicket, String dismissal) {
        return new BallInputDto(ID, ID, ID, ID, runs, wide, noBall, bye, legBye, extras, wicket, dismissal, null, null, null);
    }

    private static BallInputDto runs(int runs) {
        return ball(runs, false, false, false, false, 0, false, null);
    }

    @Test
    void validate_plainRuns_accepted() {
        assertThatCode(() -> BallRules.validate(runs(4))).doesNotThrowAnyException();
    }

    @Test
    void validate_runsOffWide_rejected() {
        assertThatThrownBy(() -> BallRules.validate(ball(2, true, false, false, false, 1, false, null)))
                .isInstanceOf(BallRejectedException.class).hasMessageContaining("wide");
    }

    @Test
    void validate_wideAndNoBall_rejected() {
        assertThatThrownBy(() -> BallRules.validate(ball(0, true, true, false, false, 1, false, null)))
                .isInstanceOf(BallRejectedException.class);
    }

    @Test
    void validate_byeWithoutRuns_rejected() {
        assertThatThrownBy(() -> BallRules.validate(ball(0, false, false, true, false, 0, false, null)))
                .isInstanceOf(BallRejectedException.class).hasMessageContaining("at least 1");
    }

    @Test
    void validate_extraRunsWithoutExtraType_rejected() {
        assertThatThrownBy(() -> BallRules.validate(ball(0, false, false, false, false, 2, false, null)))
                .isInstanceOf(BallRejectedException.class);
    }

    @Test
    void validate_wicketWithoutType_rejected() {
        assertThatThrownBy(() -> BallRules.validate(ball(0, false, false, false, false, 0, true, null)))
                .isInstanceOf(BallRejectedException.class);
    }

    @Test
    void validate_unknownDismissal_rejected() {
        assertThatThrownBy(() -> BallRules.validate(ball(0, false, false, false, false, 0, true, "EXPLODED")))
                .isInstanceOf(BallRejectedException.class);
    }

    @Test
    void validate_caughtOffNoBall_rejected() {
        assertThatThrownBy(() -> BallRules.validate(ball(0, false, true, false, false, 1, true, "CAUGHT")))
                .isInstanceOf(BallRejectedException.class).hasMessageContaining("no-ball");
    }

    @Test
    void validate_runOutOffNoBall_accepted() {
        assertThatCode(() -> BallRules.validate(ball(0, false, true, false, false, 1, true, "run_out")))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_stumpedOffWide_accepted() {
        assertThatCode(() -> BallRules.validate(ball(0, true, false, false, false, 1, true, "STUMPED")))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_dismissalTypeWithoutWicket_rejected() {
        assertThatThrownBy(() -> BallRules.validate(ball(0, false, false, false, false, 0, false, "BOWLED")))
                .isInstanceOf(BallRejectedException.class);
    }

    @Test
    void normalizedExtraRuns_wideWithZero_defaultsToPenaltyRun() {
        assertThat(BallRules.normalizedExtraRuns(ball(0, true, false, false, false, 0, false, null))).isEqualTo(1);
        assertThat(BallRules.normalizedExtraRuns(ball(0, true, false, false, false, 3, false, null))).isEqualTo(3);
        assertThat(BallRules.normalizedExtraRuns(runs(2))).isZero();
    }

    @Test
    void position_legalBalls_walkThroughAnOver() {
        assertThat(BallRules.overNumber(1, true)).isZero();
        assertThat(BallRules.ballNumber(1, true)).isEqualTo(1);
        assertThat(BallRules.overNumber(6, true)).isZero();
        assertThat(BallRules.ballNumber(6, true)).isEqualTo(6);
        assertThat(BallRules.overNumber(7, true)).isEqualTo(1);
        assertThat(BallRules.ballNumber(7, true)).isEqualTo(1);
    }

    @Test
    void position_wideAfterThreeLegalBalls_staysInSameOver() {
        assertThat(BallRules.overNumber(3, false)).isZero();
        assertThat(BallRules.ballNumber(3, false)).isEqualTo(3);
    }

    @Test
    void position_wideAfterSixLegalBalls_belongsToNextOver() {
        assertThat(BallRules.overNumber(6, false)).isEqualTo(1);
        assertThat(BallRules.ballNumber(6, false)).isZero();
    }
}
