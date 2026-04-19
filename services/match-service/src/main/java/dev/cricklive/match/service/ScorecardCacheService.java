package dev.cricklive.match.service;

import dev.cricklive.match.domain.entity.Innings;
import dev.cricklive.match.dto.InningsSummaryDto;
import dev.cricklive.match.mapper.MatchMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Writes live scorecard updates to Redis and publishes Pub/Sub notifications
 * so WebSocket gateways (commentary-service) can fan out to connected viewers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScorecardCacheService {

    private static final String CHANNEL_PATTERN = "match:%s:score";

    private final StringRedisTemplate redisTemplate;
    private final MatchMapper matchMapper;

    /**
     * Publish a scorecard snapshot to the Redis Pub/Sub channel for this match.
     * commentary-service instances subscribe and push over WebSocket.
     */
    public void publishScorecardUpdate(UUID matchId, Innings innings) {
        String channel = String.format(CHANNEL_PATTERN, matchId);
        String payload = buildPayload(innings);
        redisTemplate.convertAndSend(channel, payload);
        log.debug("Published scorecard update to channel={}", channel);
    }

    private String buildPayload(Innings innings) {
        return String.format(
                "{\"matchId\":\"%s\",\"inningsId\":\"%s\",\"runs\":%d,\"wickets\":%d,\"overs\":\"%s\",\"runRate\":%.2f}",
                innings.getMatch().getId(),
                innings.getId(),
                innings.getTotalRuns(),
                innings.getWickets(),
                innings.getOversCompleted().toPlainString(),
                innings.currentRunRate()
        );
    }
}
