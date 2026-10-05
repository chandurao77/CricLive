package dev.cricklive.scoring.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** RFC 7807 problem responses for scoring errors. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BallRejectedException.class)
    public ProblemDetail rejected(BallRejectedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(EventPublishException.class)
    public ProblemDetail publishFailed(EventPublishException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Ball was not recorded because the event bus is unavailable; retry with the same idempotencyKey.");
    }
}
