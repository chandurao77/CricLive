package dev.cricklive.scoring.exception;

/** The ball could not be published to Kafka and was not recorded (maps to HTTP 503). */
public class EventPublishException extends RuntimeException {
    public EventPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
