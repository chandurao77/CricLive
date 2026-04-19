package dev.cricklive.match.domain.entity;

import dev.cricklive.match.domain.enums.InningsStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * One innings within a Match. Maintains the running scorecard state.
 */
@Entity
@Table(name = "innings",
       uniqueConstraints = @UniqueConstraint(columnNames = {"match_id", "innings_number"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Innings extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Column(name = "innings_number", nullable = false)
    private Short inningsNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batting_team_id", nullable = false)
    private Team battingTeam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bowling_team_id", nullable = false)
    private Team bowlingTeam;

    @Column(name = "total_runs", nullable = false)
    @Builder.Default
    private Integer totalRuns = 0;

    @Column(nullable = false)
    @Builder.Default
    private Short wickets = 0;

    @Column(name = "overs_completed", nullable = false, precision = 5, scale = 1)
    @Builder.Default
    private BigDecimal oversCompleted = BigDecimal.ZERO;

    @Column(name = "extras_total", nullable = false)
    @Builder.Default
    private Integer extrasTotal = 0;

    @Column(name = "extras_wides", nullable = false)
    @Builder.Default
    private Integer extrasWides = 0;

    @Column(name = "extras_no_balls", nullable = false)
    @Builder.Default
    private Integer extrasNoBalls = 0;

    @Column(name = "extras_byes", nullable = false)
    @Builder.Default
    private Integer extrasByes = 0;

    @Column(name = "extras_leg_byes", nullable = false)
    @Builder.Default
    private Integer extrasLegByes = 0;

    @Column(name = "extras_penalty", nullable = false)
    @Builder.Default
    private Integer extrasPenalty = 0;

    @Builder.Default
    private boolean declared = false;

    @Column(name = "follow_on")
    @Builder.Default
    private boolean followOn = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InningsStatus status = InningsStatus.NOT_STARTED;

    private Integer target;

    @OneToMany(mappedBy = "innings", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BattingScorecard> battingEntries = new ArrayList<>();

    @OneToMany(mappedBy = "innings", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BowlingScorecard> bowlingEntries = new ArrayList<>();

    /** Current run rate, 0 if no overs bowled yet. */
    public double currentRunRate() {
        double overs = oversCompleted.doubleValue();
        return overs == 0 ? 0.0 : Math.round((totalRuns / overs) * 100.0) / 100.0;
    }

    /** Required run rate for the chasing team. */
    public double requiredRunRate(int totalOvers) {
        if (target == null) return 0.0;
        int runsNeeded = target - totalRuns;
        double oversLeft = totalOvers - oversCompleted.doubleValue();
        return oversLeft <= 0 ? Double.MAX_VALUE : Math.round((runsNeeded / oversLeft) * 100.0) / 100.0;
    }
}
