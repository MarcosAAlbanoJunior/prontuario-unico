package io.github.marcosaalbanojunior.prontuario.domain.model.telegram;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("PendingQuestion")
class PendingQuestionTest {

    private static final Instant ASKED = Instant.parse("2026-10-12T13:00:00Z");

    @Nested
    @DisplayName("isExpiredAt(...)")
    class IsExpiredAt {

        @ParameterizedTest(name = "{0} s after asking: expired={1}")
        @CsvSource({"0, false", "599, false", "600, true", "3600, true"})
        @DisplayName("expires ten minutes after the question")
        void should_expire_when_tenMinutesHavePassed(long seconds, boolean expired) {
            assertThat(new PendingQuestion("agua", "", ASKED).isExpiredAt(ASKED.plusSeconds(seconds))).isEqualTo(expired);
        }
    }

    @Nested
    @DisplayName("argsWithAnswer(...)")
    class ArgsWithAnswer {

        @Test
        @DisplayName("appends the answer to what was already typed")
        void should_appendAnswer_when_argsWereTyped() {
            assertThat(new PendingQuestion("dor", "4", ASKED).argsWithAnswer("joelho direito")).isEqualTo("4 joelho direito");
        }

        @Test
        @DisplayName("uses only the answer when nothing was typed")
        void should_useAnswer_when_noArgsWereTyped() {
            assertThat(new PendingQuestion("agua", "", ASKED).argsWithAnswer("500")).isEqualTo("500");
        }
    }
}
