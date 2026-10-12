package io.github.marcosaalbanojunior.prontuario.adapter.out.client.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import io.github.marcosaalbanojunior.prontuario.application.port.out.telegram.TelegramUnavailableException;
import io.github.marcosaalbanojunior.prontuario.domain.model.telegram.IncomingMessage;
import io.github.marcosaalbanojunior.prontuario.domain.model.telegram.Reply;
import io.github.marcosaalbanojunior.prontuario.domain.model.telegram.TelegramUpdate;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@DisplayName("TelegramGatewayAdapter")
class TelegramGatewayAdapterTest {

    private static final String TOKEN = "123456:SECRET-TOKEN";
    private static final String BASE = "https://api.telegram.org/bot" + TOKEN;
    private static final Duration LONG_POLL = Duration.ofSeconds(50);

    private final RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final TelegramGatewayAdapter gateway = new TelegramGatewayAdapter(builder.build());

    @Nested
    @DisplayName("fetchUpdates(...)")
    class FetchUpdates {

        @Test
        @DisplayName("long-polls for messages and maps text, non-text and non-message updates")
        void should_mapUpdates_when_telegramAnswers() {
            server.expect(requestTo(BASE + "/getUpdates"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().json("{\"offset\":5,\"timeout\":50,\"allowed_updates\":[\"message\"]}", true))
                    .andRespond(withSuccess("""
                            {"ok":true,"result":[
                              {"update_id":5,"message":{"message_id":9,"date":1760274000,"chat":{"id":42,"type":"private"},"text":"/agua 500"}},
                              {"update_id":6,"edited_message":{"message_id":9}},
                              {"update_id":7,"message":{"message_id":10,"date":1760274060,"chat":{"id":42},"photo":[]}}
                            ]}""", MediaType.APPLICATION_JSON));

            assertThat(gateway.fetchUpdates(5, LONG_POLL)).containsExactly(
                    new TelegramUpdate(5, Optional.of(new IncomingMessage(42, 9, Optional.of("/agua 500"), Instant.ofEpochSecond(1760274000)))),
                    new TelegramUpdate(6, Optional.empty()),
                    new TelegramUpdate(7, Optional.of(new IncomingMessage(42, 10, Optional.empty(), Instant.ofEpochSecond(1760274060)))));
        }

        @ParameterizedTest(name = "body {0}")
        @ValueSource(strings = {"", "{\"ok\":false,\"description\":\"Unauthorized\"}", "{\"ok\":true}"})
        @DisplayName("fails when Telegram returns no usable result")
        void should_fail_when_resultIsMissing(String body) {
            server.expect(requestTo(BASE + "/getUpdates")).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

            assertThat(catchThrowableOfType(TelegramUnavailableException.class, () -> gateway.fetchUpdates(0, LONG_POLL)))
                    .hasMessage("getUpdates returned no result");
        }

        @Test
        @DisplayName("honours retry_after when rate limited")
        void should_exposeRetryAfter_when_rateLimited() {
            server.expect(requestTo(BASE + "/getUpdates")).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"ok\":false,\"error_code\":429,\"description\":\"Too Many Requests\",\"parameters\":{\"retry_after\":17}}"));

            var error = catchThrowableOfType(TelegramUnavailableException.class, () -> gateway.fetchUpdates(0, LONG_POLL));

            assertThat(error.retryAfter()).contains(Duration.ofSeconds(17));
            assertThat(error).hasMessage("getUpdates failed with HTTP 429");
        }

        @ParameterizedTest(name = "error body {0}")
        @ValueSource(strings = {"{\"ok\":false}", "{\"ok\":false,\"parameters\":{}}", "<html>bad gateway</html>"})
        @DisplayName("has no retry hint when the error body does not carry one")
        void should_haveNoRetryAfter_when_errorBodyLacksIt(String body) {
            server.expect(requestTo(BASE + "/getUpdates"))
                    .andRespond(withStatus(HttpStatus.BAD_GATEWAY).contentType(MediaType.APPLICATION_JSON).body(body));

            assertThat(catchThrowableOfType(TelegramUnavailableException.class, () -> gateway.fetchUpdates(0, LONG_POLL))
                    .retryAfter()).isEmpty();
        }

        @Test
        @DisplayName("never exposes the bot token when the connection fails")
        void should_hideToken_when_connectionFails() {
            server.expect(requestTo(BASE + "/getUpdates")).andRespond(withException(new IOException("connection reset")));

            var error = catchThrowableOfType(TelegramUnavailableException.class, () -> gateway.fetchUpdates(0, LONG_POLL));

            assertThat(error).hasMessage("getUpdates failed: ResourceAccessException").hasNoCause();
            assertThat(error.getMessage()).doesNotContain(TOKEN);
        }
    }

    @Nested
    @DisplayName("send(...)")
    class Send {

        @Test
        @DisplayName("asks Telegram to open the reply field only for questions")
        void should_requestForceReply_when_replyIsAQuestion() {
            server.expect(requestTo(BASE + "/sendMessage"))
                    .andExpect(content().json("{\"chat_id\":42,\"text\":\"Quantos mL?\",\"reply_markup\":{\"force_reply\":true}}", true))
                    .andRespond(withSuccess("{\"ok\":true,\"result\":{}}", MediaType.APPLICATION_JSON));
            server.expect(requestTo(BASE + "/sendMessage"))
                    .andExpect(content().json("{\"chat_id\":42,\"text\":\"Registrado.\"}", true))
                    .andRespond(withSuccess("{\"ok\":true,\"result\":{}}", MediaType.APPLICATION_JSON));

            gateway.send(42, Reply.question("Quantos mL?"));
            gateway.send(42, Reply.of("Registrado."));

            server.verify();
        }
    }
}
