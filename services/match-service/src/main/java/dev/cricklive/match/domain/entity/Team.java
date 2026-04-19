package dev.cricklive.match.domain.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a cricket team (national or franchise).
 */
@Entity
@Table(name = "team")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Team extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "short_name", nullable = false, length = 10)
    private String shortName;

    @Column(name = "country_code", length = 3)
    private String countryCode;

    @Column(name = "team_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TeamType teamType;

    @Column(name = "flag_url")
    private String flagUrl;

    public enum TeamType {
        NATIONAL, FRANCHISE, ASSOCIATE
    }
}
