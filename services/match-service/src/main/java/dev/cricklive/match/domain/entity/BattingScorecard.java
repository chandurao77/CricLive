package dev.cricklive.match.domain.entity;

import dev.cricklive.match.domain.enums.DismissalType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "batting_scorecard",
       uniqueConstraints = @UniqueConstraint(columnNames = {"innings_id", "player_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BattingScorecard extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "innings_id", nullable = false)
    private Innings innings;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(nullable = false)
    private Short position;

    @Builder.Default
    private Integer runs = 0;

    @Column(name = "balls_faced")
    @Builder.Default
    private Integer ballsFaced = 0;

    @Builder.Default
    private Short fours = 0;

    @Builder.Default
    private Short sixes = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "dismissal_type", length = 30)
    private DismissalType dismissalType;

    @Column(name = "dismissed_by_bowler_id")
    private UUID dismissedByBowlerId;

    @Column(name = "dismissed_by_fielder_id")
    private UUID dismissedByFielderId;

    @Column(name = "did_not_bat")
    @Builder.Default
    private boolean didNotBat = false;

    public double strikeRate() {
        return ballsFaced == 0 ? 0.0 : Math.round((runs * 100.0 / ballsFaced) * 100.0) / 100.0;
    }
}
