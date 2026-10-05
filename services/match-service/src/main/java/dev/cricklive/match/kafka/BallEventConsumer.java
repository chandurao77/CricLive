package dev.cricklive.match.kafka;

import dev.cricklive.match.dto.BallEventMessage;
import dev.cricklive.match.service.ScoreboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Consumes ball events and projects them onto the match scorecard. */
@Component
@RequiredArgsConstructor
@Slf4j
public class BallEventConsumer {

    private final ScoreboardService scoreboardService;

    @KafkaListener(topics = "${cricklive.kafka.topics.ball-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void onBallEvent(BallEventMessage event) {
        log.debug("Ball event matchId={} over={}.{} seq={}",
                event.matchId(), event.overNumber(), event.ballNumber(), event.sequence());
        scoreboardService.applyBallEvent(event);
    }
}
