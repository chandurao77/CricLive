package dev.cricklive.match.repository;

import dev.cricklive.match.domain.entity.Match;
import dev.cricklive.match.domain.enums.MatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchRepository extends JpaRepository<Match, UUID> {

    List<Match> findByStatusOrderByScheduledStartAsc(MatchStatus status);

    Page<Match> findByStatus(MatchStatus status, Pageable pageable);

    @Query("SELECT m FROM Match m WHERE m.status IN :statuses ORDER BY m.scheduledStart ASC")
    List<Match> findByStatusIn(@Param("statuses") List<MatchStatus> statuses);

    Page<Match> findByStatusIn(List<MatchStatus> statuses, Pageable pageable);

    @Query("SELECT m FROM Match m LEFT JOIN FETCH m.homeTeam LEFT JOIN FETCH m.awayTeam LEFT JOIN FETCH m.venue WHERE m.id = :id")
    Optional<Match> findByIdWithDetails(@Param("id") UUID id);

    Page<Match> findBySeriesId(UUID seriesId, Pageable pageable);

    @Query("SELECT m FROM Match m WHERE (m.homeTeam.id = :teamId OR m.awayTeam.id = :teamId) " +
           "AND m.scheduledStart BETWEEN :from AND :to ORDER BY m.scheduledStart ASC")
    Page<Match> findByTeamAndDateRange(
            @Param("teamId") UUID teamId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );
}
