package dev.cricklive.match.repository;

import dev.cricklive.match.domain.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {

    Optional<Team> findByShortNameIgnoreCase(String shortName);

    boolean existsByShortNameIgnoreCase(String shortName);
}
