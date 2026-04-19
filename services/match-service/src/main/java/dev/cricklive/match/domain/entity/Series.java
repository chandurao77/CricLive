package dev.cricklive.match.domain.entity;

import dev.cricklive.match.domain.enums.MatchFormat;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * A cricket series or tournament (e.g., IPL 2024, India vs Australia 2024-25).
 */
@Entity
@Table(name = "series")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Series extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "short_name", length = 50)
    private String shortName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchFormat format;

    @Column(length = 10)
    private String season;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "host_country", length = 100)
    private String hostCountry;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SeriesStatus status = SeriesStatus.UPCOMING;

    public enum SeriesStatus { UPCOMING, ONGOING, COMPLETED }
}
