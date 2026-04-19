package dev.cricklive.commentary.config;

import dev.cricklive.commentary.websocket.ScorecardRedisSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.*;

/**
 * Subscribes to Redis Pub/Sub channels published by match-service.
 * Received payloads are forwarded to WebSocket clients via STOMP broker.
 */
@Configuration
public class RedisSubscriberConfig {

    private static final PatternTopic SCORE_PATTERN = new PatternTopic("match:*:score");
    private static final PatternTopic COMMENTARY_PATTERN = new PatternTopic("match:*:commentary");

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory factory,
            ScorecardRedisSubscriber scorecardSubscriber) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener(scorecardSubscriber, SCORE_PATTERN);
        container.addMessageListener(scorecardSubscriber, COMMENTARY_PATTERN);
        return container;
    }
}
