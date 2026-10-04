package dev.cricklive.match.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.cricklive.match.domain.entity.Innings;
import dev.cricklive.match.domain.entity.Match;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Publishes live scorecard snapshots to Redis Pub/Sub so commentary-service can fan them out to
 * WebSocket viewers. Publishing happens after the surrounding transaction commits, so clients that
 * react by re-fetching the match always see the new state.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScorecardCacheService {

    private static final String CHANNEL_PATTERN = "match:%s:score";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /** Queues a SCORE message for the match; sent immediately if no transaction is active. */
    public void publishScorecardUpdate(Match match, Innings innings) {
        String channel = String.format(CHANNEL_PATTERN, match.getId());
        String payload = buildPayload(match, innings);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(channel, payload);
                }
            });
        } else {
            send(channel, payload);
        }
    }

    private void send(String channel, String payload) {
        try {
            redisTemplate.convertAndSend(channel, payload);
            log.debug("Published scorecard update to channel={}", channel);
        } catch (RuntimeException ex) {
            log.warn("Could not publish scorecard update to {}: {}", channel, ex.getMessage());
        }
    }

    String buildPayload(Match match, Innings innings) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "SCORE");
        body.put("matchId", match.getId());
        body.put("inningsId", innings.getId());
        body.put("inningsNumber", innings.getInningsNumber());
        body.put("runs", innings.getTotalRuns());
        body.put("wickets", innings.getWickets());
        body.put("overs", oversText(innings.getOversCompleted()));
        body.put("runRate", innings.currentRunRate());
        body.put("target", innings.getTarget());
        body.put("matchStatus", match.getStatus());
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialise scorecard update", ex);
        }
    }

    private static String oversText(BigDecimal overs) {
        return overs == null ? "0.0" : overs.toPlainString();
    }
}
