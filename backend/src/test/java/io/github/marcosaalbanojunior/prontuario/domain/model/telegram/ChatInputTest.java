package io.github.marcosaalbanojunior.prontuario.domain.model.telegram;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("ChatInput.parse(...)")
class ChatInputTest {

    @ParameterizedTest(name = "\"{0}\" is {1}")
    @MethodSource("inputs")
    @DisplayName("splits bot commands from plain text")
    void should_splitCommandsFromPlainText_when_parsing(String text, ChatInput expected) {
        assertThat(ChatInput.parse(text)).isEqualTo(expected);
    }

    static Stream<Arguments> inputs() {
        return Stream.of(
                Arguments.of("/agua 500", new ChatInput.Command("agua", "500")),
                Arguments.of("  /AGUA   500 ml ", new ChatInput.Command("agua", "500 ml")),
                Arguments.of("/agua@ProntuarioBot 500", new ChatInput.Command("agua", "500")),
                Arguments.of("/hoje", new ChatInput.Command("hoje", "")),
                Arguments.of("joelho direito", new ChatInput.PlainText("joelho direito")),
                Arguments.of("acordei com dor / cansado", new ChatInput.PlainText("acordei com dor / cansado")),
                Arguments.of("500", new ChatInput.PlainText("500")));
    }
}
