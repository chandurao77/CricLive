package dev.cricklive.match.domain.entity;

import jakarta.persistence.*;
import lombok.*;

/** A squad member. Scorecards reference players by id; this table resolves their display name. */
@Entity
@Table(name = "player")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(length = 30)
    private String role;
}
