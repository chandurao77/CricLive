package dev.cricklive.match.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/** Retries a failing ball event a few times (e.g. innings not created yet), then drops it with an error log. */
@Configuration
@Slf4j
public class KafkaConsumerConfig {

    private static final long RETRY_INTERVAL_MS = 1_000;
    private static final long MAX_RETRIES = 3;

    @Bean
    DefaultErrorHandler kafkaErrorHandler() {
        DefaultErrorHandler handler = new DefaultErrorHandler(
                (record, ex) -> log.error("Dropping ball event after retries: {}", record.value(), ex),
                new FixedBackOff(RETRY_INTERVAL_MS, MAX_RETRIES));
        return handler;
    }
}
