package dev.cricklive.match.scoring;

import dev.cricklive.match.domain.entity.BattingScorecard;
import dev.cricklive.match.domain.entity.BowlingScorecard;
import dev.cricklive.match.domain.entity.Innings;
import dev.cricklive.match.domain.enums.DismissalType;
import dev.cricklive.match.domain.enums.InningsStatus;
import dev.cricklive.match.dto.BallEventMessage;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Pure scoring rules: applies one delivery to an innings aggregate (totals, extras, overs,
 * batting and bowling cards). Holds no state and does no I/O.
 */
@Component
public class InningsScorer {

    static final int BALLS_PER_OVER = 6;
    static final int WICKETS_PER_INNINGS = 10;
    private static final int PENALTY_RUN = 1;
    private static final int FOUR = 4;
    private static final int SIX = 6;

    /**
     * Applies a delivery to the innings. Does nothing and returns false when the innings is not in
     * progress or the event was already applied (sequence not newer than the last one applied).
     */
    public boolean apply(Innings innings, BallEventMessage e) {
        if (innings.getStatus() != InningsStatus.IN_PROGRESS || e.sequence() <= innings.getLastEventSeq()) {
            return false;
        }

        innings.setTotalRuns(innings.getTotalRuns() + e.runsScored() + e.extraRuns());
        addExtras(innings, e);
        if (e.legalBall()) {
            innings.setOversCompleted(oversFromBalls(e.legalBallsInInnings()));
        }
        updateBatting(innings, e);
        updateBowling(innings, e);
        if (e.wicket()) {
            recordWicket(innings, e);
        }
        innings.setLastEventSeq(e.sequence());
        return true;
    }

    /** True when the innings has reached a natural end (all out, overs bowled, or target reached). */
    public boolean isOver(Innings innings, int maxOvers) {
        boolean allOut = innings.getWickets() >= WICKETS_PER_INNINGS;
        boolean oversDone = maxOvers > 0 && innings.legalBalls() >= maxOvers * BALLS_PER_OVER;
        boolean targetReached = innings.getTarget() != null && innings.getTotalRuns() >= innings.getTarget();
        return allOut || oversDone || targetReached;
    }

    static BigDecimal oversFromBalls(int balls) {
        return new BigDecimal(balls / BALLS_PER_OVER + "." + balls % BALLS_PER_OVER);
    }

    static int ballsFromOvers(BigDecimal overs) {
        return overs.intValue() * BALLS_PER_OVER + overs.remainder(BigDecimal.ONE).movePointRight(1).intValue();
    }

    private void addExtras(Innings innings, BallEventMessage e) {
        int extra = e.extraRuns();
        if (extra == 0) {
            return;
        }
        if (e.bye()) {
            splitPenalty(innings, e, extra);
            innings.setExtrasByes(innings.getExtrasByes() + extra - penaltyOf(e));
        } else if (e.legBye()) {
            splitPenalty(innings, e, extra);
            innings.setExtrasLegByes(innings.getExtrasLegByes() + extra - penaltyOf(e));
        } else if (e.wide()) {
            innings.setExtrasWides(innings.getExtrasWides() + extra);
        } else if (e.noBall()) {
            innings.setExtrasNoBalls(innings.getExtrasNoBalls() + extra);
        }
        innings.setExtrasTotal(innings.getExtrasWides() + innings.getExtrasNoBalls()
                + innings.getExtrasByes() + innings.getExtrasLegByes() + innings.getExtrasPenalty());
    }

    /** For byes/leg-byes taken off a wide or no-ball, the first run is the wide/no-ball penalty. */
    private void splitPenalty(Innings innings, BallEventMessage e, int extra) {
        if (e.wide()) {
            innings.setExtrasWides(innings.getExtrasWides() + PENALTY_RUN);
        } else if (e.noBall()) {
            innings.setExtrasNoBalls(innings.getExtrasNoBalls() + PENALTY_RUN);
        }
    }

    private int penaltyOf(BallEventMessage e) {
        return (e.wide() || e.noBall()) ? PENALTY_RUN : 0;
    }

    private void updateBatting(Innings innings, BallEventMessage e) {
        BattingScorecard batter = battingEntry(innings, e.batterId());
        if (!e.wide()) {
            batter.setBallsFaced(batter.getBallsFaced() + 1);
        }
        batter.setRuns(batter.getRuns() + e.runsScored());
        if (e.runsScored() == FOUR) {
            batter.setFours((short) (batter.getFours() + 1));
        } else if (e.runsScored() == SIX) {
            batter.setSixes((short) (batter.getSixes() + 1));
        }
    }

    private void updateBowling(Innings innings, BallEventMessage e) {
        BowlingScorecard bowler = bowlingEntry(innings, e.bowlerId());
        int conceded = e.runsScored();
        if (e.wide()) {
            conceded += e.extraRuns();
            bowler.setWides((short) (bowler.getWides() + e.extraRuns()));
        } else if (e.noBall()) {
            conceded += PENALTY_RUN;
            bowler.setNoBalls((short) (bowler.getNoBalls() + 1));
        }
        bowler.setRuns(bowler.getRuns() + conceded);
        bowler.setCurrentOverRuns((short) (bowler.getCurrentOverRuns() + conceded));
        if (e.legalBall()) {
            int balls = ballsFromOvers(bowler.getOvers()) + 1;
            bowler.setOvers(oversFromBalls(balls));
            if (balls % BALLS_PER_OVER == 0) {
                if (bowler.getCurrentOverRuns() == 0) {
                    bowler.setMaidens((short) (bowler.getMaidens() + 1));
                }
                bowler.setCurrentOverRuns((short) 0);
            }
        }
    }

    private void recordWicket(Innings innings, BallEventMessage e) {
        UUID outId = e.dismissedBatterId() != null ? e.dismissedBatterId() : e.batterId();
        DismissalType type = DismissalType.valueOf(e.dismissalType());
        BattingScorecard out = battingEntry(innings, outId);
        boolean bowlerCredited = type != DismissalType.RUN_OUT;
        out.setDismissalType(type);
        out.setDismissedByBowlerId(bowlerCredited ? e.bowlerId() : null);
        out.setDismissedByFielderId(e.fielderId());
        innings.setWickets((short) (innings.getWickets() + 1));
        if (bowlerCredited) {
            BowlingScorecard bowler = bowlingEntry(innings, e.bowlerId());
            bowler.setWickets((short) (bowler.getWickets() + 1));
        }
    }

    private BattingScorecard battingEntry(Innings innings, UUID playerId) {
        return innings.getBattingEntries().stream()
                .filter(b -> b.getPlayerId().equals(playerId))
                .findFirst()
                .orElseGet(() -> {
                    BattingScorecard created = BattingScorecard.builder()
                            .innings(innings)
                            .playerId(playerId)
                            .position((short) (innings.getBattingEntries().size() + 1))
                            .build();
                    innings.getBattingEntries().add(created);
                    return created;
                });
    }

    private BowlingScorecard bowlingEntry(Innings innings, UUID playerId) {
        return innings.getBowlingEntries().stream()
                .filter(b -> b.getPlayerId().equals(playerId))
                .findFirst()
                .orElseGet(() -> {
                    BowlingScorecard created = BowlingScorecard.builder()
                            .innings(innings)
                            .playerId(playerId)
                            .build();
                    innings.getBowlingEntries().add(created);
                    return created;
                });
    }
}
