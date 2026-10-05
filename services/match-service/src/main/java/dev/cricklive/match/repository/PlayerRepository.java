package dev.cricklive.match.repository;

import dev.cricklive.match.domain.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlayerRepository extends JpaRepository<Player, UUID> {

    List<Player> findByTeamIdOrderByNameAsc(UUID teamId);
}
