package dev.cricklive.scoring.service;

import dev.cricklive.scoring.dto.BallInputDto;
import dev.cricklive.scoring.exception.BallRejectedException;

import java.util.Set;

/** Pure cricket-rule validation and derivation for a single delivery. */
final class BallRules {

    static final Set<String> DISMISSALS =
            Set.of("BOWLED", "CAUGHT", "LBW", "RUN_OUT", "STUMPED", "HIT_WICKET");
    private static final Set<String> WIDE_DISMISSALS = Set.of("STUMPED", "RUN_OUT", "HIT_WICKET");
    private static final Set<String> NO_BALL_DISMISSALS = Set.of("RUN_OUT");
    private static final int BALLS_PER_OVER = 6;

    private BallRules() {}

    /** Throws {@link BallRejectedException} if the delivery contradicts the laws of the game. */
    static void validate(BallInputDto in) {
        if (in.wide() && in.noBall()) {
            throw new BallRejectedException("A delivery cannot be both a wide and a no-ball");
        }
        if (in.wide() && in.runsScored() > 0) {
            throw new BallRejectedException("The batter cannot score runs off a wide");
        }
        if (in.bye() && in.legBye()) {
            throw new BallRejectedException("A delivery cannot be both byes and leg-byes");
        }
        if ((in.bye() || in.legBye()) && in.runsScored() > 0) {
            throw new BallRejectedException("The batter cannot score runs off byes or leg-byes");
        }
        if ((in.bye() || in.legBye()) && in.extraRuns() < 1) {
            throw new BallRejectedException("Byes and leg-byes need extraRuns of at least 1");
        }
        if (!in.wide() && !in.noBall() && !in.bye() && !in.legBye() && in.extraRuns() > 0) {
            throw new BallRejectedException("extraRuns given but no extra type (wide/noBall/bye/legBye) is set");
        }
        validateDismissal(in);
    }

    private static void validateDismissal(BallInputDto in) {
        if (!in.wicket()) {
            if (in.dismissalType() != null && !in.dismissalType().isBlank()) {
                throw new BallRejectedException("dismissalType given but wicket is false");
            }
            return;
        }
        String type = in.dismissalType() == null ? "" : in.dismissalType().toUpperCase();
        if (!DISMISSALS.contains(type)) {
            throw new BallRejectedException("dismissalType must be one of " + DISMISSALS);
        }
        if (in.wide() && !WIDE_DISMISSALS.contains(type)) {
            throw new BallRejectedException(type + " is not a valid dismissal off a wide");
        }
        if (in.noBall() && !NO_BALL_DISMISSALS.contains(type)) {
            throw new BallRejectedException(type + " is not a valid dismissal off a no-ball");
        }
    }

    /** Wides and no-balls always concede at least the one-run penalty. */
    static int normalizedExtraRuns(BallInputDto in) {
        return (in.wide() || in.noBall()) ? Math.max(1, in.extraRuns()) : in.extraRuns();
    }

    static boolean isLegal(BallInputDto in) {
        return !in.wide() && !in.noBall();
    }

    /** Over (0-indexed) a delivery belongs to, given legal balls bowled in the innings including this one. */
    static int overNumber(int legalBallsAfter, boolean legal) {
        return legal ? (legalBallsAfter - 1) / BALLS_PER_OVER : legalBallsAfter / BALLS_PER_OVER;
    }

    /** Ball within the over: 1..6 for a legal ball, else the legal balls already bowled in that over. */
    static int ballNumber(int legalBallsAfter, boolean legal) {
        return legal ? (legalBallsAfter - 1) % BALLS_PER_OVER + 1 : legalBallsAfter % BALLS_PER_OVER;
    }
}
