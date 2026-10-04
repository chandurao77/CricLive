package dev.cricklive.commentary.service;

import dev.cricklive.commentary.domain.CommentaryDocument;
import dev.cricklive.commentary.domain.CommentaryDocument.CommentaryEventType;
import dev.cricklive.commentary.dto.BallEventMessage;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CommentaryGeneratorServiceTest {

    private final CommentaryGeneratorService generator = new CommentaryGeneratorService();

    private BallEventMessage event(int runs, boolean wicket, String dismissal, boolean wide, boolean noBall,
                                   boolean bye, boolean legBye, int extras) {
        return new BallEventMessage("evt-1", 1, UUID.randomUUID(), UUID.randomUUID(), 3, 2, !wide && !noBall,
                runs, wicket, dismissal, wide, noBall, bye, legBye, extras, runs == 4 || runs == 6, runs == 6, Instant.now());
    }

    @Test
    void generate_six_isSixType() {
        CommentaryDocument doc = generator.generate(event(6, false, null, false, false, false, false, 0));
        assertThat(doc.getEventType()).isEqualTo(CommentaryEventType.SIX);
        assertThat(doc.getTags()).contains("SIX", "BOUNDARY");
        assertThat(doc.getBallEventId()).isEqualTo("evt-1");
        assertThat(doc.getOverNumber()).isEqualTo(3);
        assertThat(doc.getBallNumber()).isEqualTo(2);
    }

    @Test
    void generate_four_isBoundaryType() {
        assertThat(generator.generate(event(4, false, null, false, false, false, false, 0)).getEventType())
                .isEqualTo(CommentaryEventType.BOUNDARY);
    }

    @Test
    void generate_wicket_mentionsDismissalMode() {
        CommentaryDocument doc = generator.generate(event(0, true, "LBW", false, false, false, false, 0));
        assertThat(doc.getEventType()).isEqualTo(CommentaryEventType.WICKET);
        assertThat(doc.getText()).contains("LBW");
    }

    @Test
    void generate_wide_reportsExtras() {
        CommentaryDocument doc = generator.generate(event(0, false, null, true, false, false, false, 5));
        assertThat(doc.getEventType()).isEqualTo(CommentaryEventType.WIDE);
        assertThat(doc.getText()).contains("5 runs");
    }

    @Test
    void generate_singleRun_usesSingular() {
        assertThat(generator.generate(event(1, false, null, false, false, false, false, 0)).getText())
                .isEqualTo("1 run taken.");
    }

    @Test
    void generate_byes_mentionsByes() {
        assertThat(generator.generate(event(0, false, null, false, false, true, false, 2)).getText())
                .startsWith("Byes!");
    }

    @Test
    void escapeHtml_neutralisesMarkup() {
        assertThat(CommentaryGeneratorService.escapeHtml("<b>\"x\" & y</b>"))
                .isEqualTo("&lt;b&gt;&quot;x&quot; &amp; y&lt;/b&gt;");
    }
}
