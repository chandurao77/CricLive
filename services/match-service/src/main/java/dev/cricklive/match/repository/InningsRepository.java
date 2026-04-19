package dev.cricklive.match.repository;

import dev.cricklive.match.domain.entity.Innings;
import dev.cricklive.match.domain.enums.InningsStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InningsRepository extends JpaRepository<Innings, UUID> {

    List<Innings> findByMatchIdOrderByInningsNumberAsc(UUID matchId);

    Optional<Innings> findByMatchIdAndStatus(UUID matchId, InningsStatus status);

    @Query("SELECT i FROM Innings i JOIN FETCH i.battingEntries JOIN FETCH i.bowlingEntries WHERE i.id = :id")
    Optional<Innings> findByIdWithScorecard(@Param("id") UUID id);
}
