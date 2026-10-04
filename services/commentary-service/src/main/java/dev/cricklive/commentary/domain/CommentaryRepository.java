package dev.cricklive.commentary.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentaryRepository extends MongoRepository<CommentaryDocument, String> {

    /** Latest N commentary entries for a match — used for reconnect catch-up. */
    List<CommentaryDocument> findByMatchIdOrderByTimestampDesc(UUID matchId, Pageable pageable);

    /** All commentary for a specific innings, chronological. */
    List<CommentaryDocument> findByMatchIdAndInningsIdOrderByOverNumberAscBallNumberAsc(
            UUID matchId, UUID inningsId);

    /** Commentary for a specific over. */
    List<CommentaryDocument> findByMatchIdAndInningsIdAndOverNumberOrderByBallNumberAsc(
            UUID matchId, UUID inningsId, int overNumber);

    boolean existsByBallEventId(String ballEventId);

    Page<CommentaryDocument> findByMatchId(UUID matchId, Pageable pageable);
}
