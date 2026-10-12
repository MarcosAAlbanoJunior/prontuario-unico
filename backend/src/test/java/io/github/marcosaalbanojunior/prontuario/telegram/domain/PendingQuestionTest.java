package io.github.marcosaalbanojunior.prontuario.telegram.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class PendingQuestionTest {

    private static final Instant ASKED = Instant.parse("2026-10-12T13:00:00Z");

    @Test
    void shouldExpireAfterTenMinutes() {
        var question = new PendingQuestion("agua", "", ASKED);

        assertThat(question.isExpiredAt(ASKED.plusSeconds(599))).isFalse();
        assertThat(question.isExpiredAt(ASKED.plusSeconds(600))).isTrue();
    }

    @Test
    void shouldAppendTheAnswerToWhatWasAlreadyTyped() {
        assertThat(new PendingQuestion("dor", "4", ASKED).argsWithAnswer("joelho direito")).isEqualTo("4 joelho direito");
        assertThat(new PendingQuestion("agua", "", ASKED).argsWithAnswer("500")).isEqualTo("500");
    }
}
