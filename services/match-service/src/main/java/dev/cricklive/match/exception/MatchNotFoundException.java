package dev.cricklive.match.exception;

import java.util.UUID;

public class MatchNotFoundException extends RuntimeException {

    public MatchNotFoundException(UUID matchId) {
        super("Match not found: " + matchId);
    }
}
