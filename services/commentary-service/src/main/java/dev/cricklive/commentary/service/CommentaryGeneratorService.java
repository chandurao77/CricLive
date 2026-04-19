package dev.cricklive.commentary.service;

import dev.cricklive.commentary.domain.CommentaryDocument;
import dev.cricklive.commentary.domain.CommentaryDocument.CommentaryEventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Generates human-readable commentary text from a ball event map.
 * In production this can be enhanced with an LLM or a template engine.
 */
@Service
@Slf4j
public class CommentaryGeneratorService {

    private static final Random RNG = new Random();

    public CommentaryDocument generate(Map<String, Object> event) {
        int runsScored = toInt(event.get("runsScored"));
        boolean isWicket = toBool(event.get("isWicket"));
        boolean isWide = toBool(event.get("isWide"));
        boolean isNoBall = toBool(event.get("isNoBall"));
        boolean isBoundary = runsScored == 4;
        boolean isSix = runsScored == 6;

        CommentaryEventType type = resolveEventType(runsScored, isWicket, isWide, isNoBall);
        String text = buildText(event, type, runsScored, isWicket, isWide, isNoBall, isBoundary, isSix);

        return CommentaryDocument.builder()
                .matchId(UUID.fromString((String) event.get("matchId")))
                .inningsId(UUID.fromString((String) event.get("inningsId")))
                .overNumber(toInt(event.get("overNumber")))
                .ballNumber(toInt(event.get("ballNumber")))
                .text(text)
                .htmlText(toHtml(text, type))
                .eventType(type)
                .tags(buildTags(type, isBoundary, isSix))
                .language("en")
                .timestamp(Instant.now())
                .author("system")
                .build();
    }

    private CommentaryEventType resolveEventType(int runs, boolean wicket, boolean wide, boolean noBall) {
        if (wicket) return CommentaryEventType.WICKET;
        if (runs == 6) return CommentaryEventType.SIX;
        if (runs == 4) return CommentaryEventType.BOUNDARY;
        if (wide) return CommentaryEventType.WIDE;
        if (noBall) return CommentaryEventType.NO_BALL;
        return CommentaryEventType.NORMAL;
    }

    private String buildText(Map<String, Object> event, CommentaryEventType type,
                             int runs, boolean wicket, boolean wide, boolean noBall,
                             boolean boundary, boolean six) {
        String bowler = (String) event.getOrDefault("bowlerId", "Bowler");
        String batter = (String) event.getOrDefault("batterId", "Batter");

        return switch (type) {
            case WICKET -> pickRandom(
                "OUT! What a breakthrough! The batter is dismissed.",
                "WICKET! The bowler strikes at the crucial moment!",
                "That's OUT! The fielding side is ecstatic!"
            );
            case SIX -> pickRandom(
                "SIX! Massive hit! The ball sails over the ropes!",
                "SIX! That's gone into the stands! What a shot!",
                "MAXIMUM! Effortless power hitting, " + runs + " runs!"
            );
            case BOUNDARY -> pickRandom(
                "FOUR! Lovely timing through the covers.",
                "FOUR! That raced away to the boundary!",
                "FOUR! Beautiful strokeplay — well timed."
            );
            case WIDE -> "Wide! The bowler strays outside the tramline. Extras: 1.";
            case NO_BALL -> "No Ball! The bowler overstepped. Free hit on the next delivery.";
            default -> runs == 0
                ? pickRandom("Dot ball. Tight line from the bowler.", "No run. Good delivery.", "Defended solidly.")
                : runs + (runs == 1 ? " run taken." : " runs scored.");
        };
    }

    private String toHtml(String text, CommentaryEventType type) {
        String prefix = switch (type) {
            case WICKET -> "<span class='badge badge-red'>OUT</span> ";
            case SIX -> "<span class='badge badge-purple'>SIX</span> ";
            case BOUNDARY -> "<span class='badge badge-green'>FOUR</span> ";
            default -> "";
        };
        return prefix + text;
    }

    private List<String> buildTags(CommentaryEventType type, boolean boundary, boolean six) {
        List<String> tags = new ArrayList<>();
        tags.add(type.name());
        if (boundary) tags.add("BOUNDARY");
        if (six) tags.add("SIX");
        return tags;
    }

    private String pickRandom(String... options) {
        return options[RNG.nextInt(options.length)];
    }

    private int toInt(Object val) {
        if (val instanceof Number n) return n.intValue();
        return 0;
    }

    private boolean toBool(Object val) {
        if (val instanceof Boolean b) return b;
        return false;
    }
}
