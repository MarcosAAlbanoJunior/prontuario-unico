package io.github.marcosaalbanojunior.prontuario.telegram.adapter.out.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import io.github.marcosaalbanojunior.prontuario.telegram.application.port.out.TelegramUnavailableException;
import io.github.marcosaalbanojunior.prontuario.telegram.domain.IncomingMessage;
import io.github.marcosaalbanojunior.prontuario.telegram.domain.Reply;
import io.github.marcosaalbanojunior.prontuario.telegram.domain.TelegramUpdate;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class TelegramHttpGatewayTest {

    private static final String TOKEN = "123456:SECRET-TOKEN";
    private static final String BASE = "https://api.telegram.org/bot" + TOKEN;

    private final RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final TelegramHttpGateway gateway = new TelegramHttpGateway(builder.build());

    @Test
    void shouldLongPollForMessagesAndMapThem() {
        server.expect(requestTo(BASE + "/getUpdates"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"offset\":5,\"timeout\":50,\"allowed_updates\":[\"message\"]}", true))
                .andRespond(withSuccess("""
                        {"ok":true,"result":[
                          {"update_id":5,"message":{"message_id":9,"date":1760274000,"chat":{"id":42,"type":"private"},"text":"/agua 500"}},
                          {"update_id":6,"edited_message":{"message_id":9}},
                          {"update_id":7,"message":{"message_id":10,"date":1760274060,"chat":{"id":42},"photo":[]}}
                        ]}""", MediaType.APPLICATION_JSON));

        var updates = gateway.fetchUpdates(5, Duration.ofSeconds(50));

        assertThat(updates).containsExactly(
                new TelegramUpdate(5, Optional.of(new IncomingMessage(42, 9, Optional.of("/agua 500"), Instant.ofEpochSecond(1760274000)))),
                new TelegramUpdate(6, Optional.empty()),
                new TelegramUpdate(7, Optional.of(new IncomingMessage(42, 10, Optional.empty(), Instant.ofEpochSecond(1760274060)))));
    }

    @Test
    void shouldAskTelegramToOpenTheReplyFieldForQuestions() {
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

    @Test
    void shouldHonourRetryAfterWhenRateLimited() {
        server.expect(requestTo(BASE + "/getUpdates")).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"ok\":false,\"error_code\":429,\"description\":\"Too Many Requests\",\"parameters\":{\"retry_after\":17}}"));

        var error = catchThrowableOfType(TelegramUnavailableException.class, () -> gateway.fetchUpdates(0, Duration.ofSeconds(50)));

        assertThat(error.retryAfter()).contains(Duration.ofSeconds(17));
    }

    @Test
    void shouldNeverExposeTheBotTokenInErrors() {
        server.expect(requestTo(BASE + "/getUpdates")).andRespond(withException(new IOException("connection reset")));

        var error = catchThrowableOfType(TelegramUnavailableException.class, () -> gateway.fetchUpdates(0, Duration.ofSeconds(50)));

        assertThat(error).hasMessageNotContaining(TOKEN).hasNoCause();
        assertThat(error.retryAfter()).isEmpty();
    }
}
