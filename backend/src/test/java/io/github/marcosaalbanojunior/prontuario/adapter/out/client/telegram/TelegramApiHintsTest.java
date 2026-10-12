package io.github.marcosaalbanojunior.prontuario.adapter.out.client.telegram;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

@DisplayName("TelegramApiHints")
class TelegramApiHintsTest {

    @ParameterizedTest(name = "{0} is registered for reflection")
    @MethodSource("records")
    @DisplayName("registers every Bot API record Jackson reads or writes in the native image")
    void should_registerReflection_when_recordIsSerialized(Class<?> record) {
        var hints = new RuntimeHints();

        new TelegramApiHints().registerHints(hints, getClass().getClassLoader());

        assertThat(RuntimeHintsPredicates.reflection().onType(record)).accepts(hints);
    }

    static Stream<Class<?>> records() {
        return Stream.of(TelegramApi.Response.class, TelegramApi.Parameters.class, TelegramApi.Update.class,
                TelegramApi.Message.class, TelegramApi.Chat.class, TelegramApi.GetUpdatesRequest.class,
                TelegramApi.SendMessageRequest.class, TelegramApi.ForceReply.class);
    }
}
