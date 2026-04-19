package dev.cricklive.match.domain.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Cricket ground / venue details.
 */
@Entity
@Table(name = "venue")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Venue extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "short_name", length = 80)
    private String shortName;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String country;

    private Integer capacity;

    @Column(name = "pitch_type", length = 50)
    private String pitchType;

    @Column(precision = 9, scale = 6)
    private java.math.BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private java.math.BigDecimal longitude;

    @Column(length = 50)
    private String timezone;
}
