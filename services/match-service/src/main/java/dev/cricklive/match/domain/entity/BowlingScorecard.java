package dev.cricklive.match.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "bowling_scorecard",
       uniqueConstraints = @UniqueConstraint(columnNames = {"innings_id", "player_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BowlingScorecard extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "innings_id", nullable = false)
    private Innings innings;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(precision = 5, scale = 1)
    @Builder.Default
    private BigDecimal overs = BigDecimal.ZERO;

    @Builder.Default
    private Short maidens = 0;

    @Builder.Default
    private Integer runs = 0;

    @Builder.Default
    private Short wickets = 0;

    @Builder.Default
    private Short wides = 0;

    @Column(name = "no_balls")
    @Builder.Default
    private Short noBalls = 0;

    public double economy() {
        double o = overs.doubleValue();
        return o == 0 ? 0.0 : Math.round((runs / o) * 100.0) / 100.0;
    }
}
