package dev.cricklive.scoring.exception;

/** The submitted delivery is internally inconsistent (maps to HTTP 400). */
public class BallRejectedException extends RuntimeException {
    public BallRejectedException(String message) {
        super(message);
    }
}
