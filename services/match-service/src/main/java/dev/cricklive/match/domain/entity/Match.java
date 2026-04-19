package dev.cricklive.match.domain.entity;

import dev.cricklive.match.domain.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root for a single cricket match.
 */
@Entity
@Table(name = "match")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id")
    private Series series;

    @Column(name = "match_number")
    private Integer matchNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchFormat format;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    private Venue venue;

    @Column(name = "scheduled_start", nullable = false)
    private Instant scheduledStart;

    @Column(name = "actual_start")
    private Instant actualStart;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MatchStatus status = MatchStatus.UPCOMING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_team_id")
    private Team homeTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "away_team_id")
    private Team awayTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "toss_winner_id")
    private Team tossWinner;

    @Enumerated(EnumType.STRING)
    @Column(name = "toss_decision", length = 10)
    private TossDecision tossDecision;

    @Column(name = "result_type", length = 20)
    @Enumerated(EnumType.STRING)
    private ResultType resultType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winning_team_id")
    private Team winningTeam;

    @Column(name = "win_margin")
    private Integer winMargin;

    @Column(name = "win_margin_unit", length = 10)
    @Enumerated(EnumType.STRING)
    private WinMarginUnit winMarginUnit;

    @Column(name = "dls_applied")
    @Builder.Default
    private boolean dlsApplied = false;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("inningsNumber ASC")
    @Builder.Default
    private List<Innings> innings = new ArrayList<>();

    public enum ResultType { WIN, DRAW, TIE, NO_RESULT, ABANDONED }
    public enum WinMarginUnit { RUNS, WICKETS }

    /** Convenience: update status and set actualStart if transitioning to LIVE. */
    public void transitionTo(MatchStatus newStatus) {
        if (newStatus == MatchStatus.LIVE && this.actualStart == null) {
            this.actualStart = Instant.now();
        }
        this.status = newStatus;
    }

    public Innings currentInnings() {
        return innings.stream()
                .filter(i -> i.getStatus() == InningsStatus.IN_PROGRESS)
                .findFirst()
                .orElse(null);
    }
}
