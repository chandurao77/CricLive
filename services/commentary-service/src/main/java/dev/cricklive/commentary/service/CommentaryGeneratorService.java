package dev.cricklive.commentary.service;

import dev.cricklive.commentary.domain.CommentaryDocument;
import dev.cricklive.commentary.domain.CommentaryDocument.CommentaryEventType;
import dev.cricklive.commentary.dto.BallEventMessage;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates human-readable commentary text from a ball event.
 * Template-based; can be swapped for an LLM or richer templating later.
 */
@Service
public class CommentaryGeneratorService {

    private static final Random RNG = new Random();

    public CommentaryDocument generate(BallEventMessage e) {
        CommentaryEventType type = resolveEventType(e);
        String text = buildText(e, type);

        return CommentaryDocument.builder()
                .matchId(e.matchId())
                .inningsId(e.inningsId())
                .overNumber(e.overNumber())
                .ballNumber(e.ballNumber())
                .ballEventId(e.eventId())
                .text(text)
                .htmlText(escapeHtml(text))
                .eventType(type)
                .tags(buildTags(type, e))
                .language("en")
                .timestamp(e.timestamp() != null ? e.timestamp() : Instant.now())
                .author("system")
                .build();
    }

    CommentaryEventType resolveEventType(BallEventMessage e) {
        if (e.wicket()) return CommentaryEventType.WICKET;
        if (e.six()) return CommentaryEventType.SIX;
        if (e.boundary()) return CommentaryEventType.BOUNDARY;
        if (e.wide()) return CommentaryEventType.WIDE;
        if (e.noBall()) return CommentaryEventType.NO_BALL;
        return CommentaryEventType.NORMAL;
    }

    String buildText(BallEventMessage e, CommentaryEventType type) {
        return switch (type) {
            case WICKET -> pickRandom(
                "OUT! " + dismissalPhrase(e.dismissalType()) + " The fielding side is ecstatic!",
                "WICKET! " + dismissalPhrase(e.dismissalType()) + " A crucial breakthrough!",
                "That's OUT! " + dismissalPhrase(e.dismissalType()));
            case SIX -> pickRandom(
                "SIX! Massive hit! The ball sails over the ropes!",
                "SIX! That's gone into the stands! What a shot!",
                "MAXIMUM! Effortless power hitting.");
            case BOUNDARY -> pickRandom(
                "FOUR! Lovely timing through the covers.",
                "FOUR! That raced away to the boundary!",
                "FOUR! Beautiful strokeplay, well timed.");
            case WIDE -> "Wide! The bowler strays down the leg side. " + plural(e.extraRuns(), "run") + " to the extras.";
            case NO_BALL -> "No ball! The bowler overstepped. Free hit next. "
                    + (e.runsScored() > 0 ? plural(e.runsScored(), "run") + " off the bat plus the no-ball." : "1 run to the extras.");
            default -> normalText(e);
        };
    }

    private String normalText(BallEventMessage e) {
        if (e.bye()) return "Byes! " + plural(e.extraRuns(), "run") + " taken as the keeper misses it.";
        if (e.legBye()) return "Leg byes. " + plural(e.extraRuns(), "run") + " off the pads.";
        if (e.runsScored() == 0) {
            return pickRandom("Dot ball. Tight line from the bowler.", "No run. Good delivery.", "Defended solidly.");
        }
        return plural(e.runsScored(), "run") + (e.runsScored() == 1 ? " taken." : " scored.");
    }

    private String dismissalPhrase(String type) {
        if (type == null) return "The batter is dismissed.";
        return switch (type) {
            case "BOWLED" -> "Bowled! The stumps are shattered.";
            case "CAUGHT" -> "Caught! Straight down the fielder's throat.";
            case "LBW" -> "LBW! Plumb in front.";
            case "RUN_OUT" -> "Run out! A mix-up in the middle.";
            case "STUMPED" -> "Stumped! Out of the crease and the keeper does the rest.";
            case "HIT_WICKET" -> "Hit wicket! The batter has dislodged the bails.";
            default -> "The batter is dismissed.";
        };
    }

    private List<String> buildTags(CommentaryEventType type, BallEventMessage e) {
        List<String> tags = new ArrayList<>();
        tags.add(type.name());
        if (e.boundary()) tags.add("BOUNDARY");
        if (e.six()) tags.add("SIX");
        return tags;
    }

    static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String plural(int n, String word) {
        return n + " " + word + (n == 1 ? "" : "s");
    }

    private String pickRandom(String... options) {
        return options[RNG.nextInt(options.length)];
    }
}
