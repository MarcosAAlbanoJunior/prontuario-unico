package io.github.marcosaalbanojunior.prontuario.domain.model.telegram;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Reply")
class ReplyTest {

    @Test
    @DisplayName("of(...) is a plain message and question(...) asks for an answer")
    void should_expectAnswerOnlyForQuestions_when_created() {
        assertThat(Reply.of("ok").expectsAnswer()).isFalse();
        assertThat(Reply.question("Quantos mL?").expectsAnswer()).isTrue();
    }
}
