package io.github.marcosaalbanojunior.prontuario.telegram.adapter.out.http;

import io.github.marcosaalbanojunior.prontuario.telegram.application.port.out.TelegramGateway;
import io.github.marcosaalbanojunior.prontuario.telegram.application.port.out.TelegramUnavailableException;
import io.github.marcosaalbanojunior.prontuario.telegram.domain.Reply;
import io.github.marcosaalbanojunior.prontuario.telegram.domain.TelegramUpdate;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Plain RestClient calls to the Bot API. The bot token is part of the base URL, and Spring's I/O
 * exceptions quote the URL, so failures are rethrown without their cause to keep the token out of logs.
 */
public final class TelegramHttpGateway implements TelegramGateway {

    private static final ParameterizedTypeReference<TelegramApi.Response<List<TelegramApi.Update>>> UPDATES =
            new ParameterizedTypeReference<>() {};

    private final RestClient http;

    /** {@code http} must have the base URL {@code <api-url>/bot<token>}. */
    public TelegramHttpGateway(RestClient http) {
        this.http = http;
    }

    @Override
    public List<TelegramUpdate> fetchUpdates(long offset, Duration longPoll) {
        var request = new TelegramApi.GetUpdatesRequest(offset, (int) longPoll.toSeconds(), TelegramApi.MESSAGE_UPDATES_ONLY);
        var response = call("getUpdates", () -> http.post().uri("/getUpdates").body(request).retrieve().body(UPDATES));
        if (response == null || !response.ok() || response.result() == null) {
            throw new TelegramUnavailableException("getUpdates returned no result", Optional.empty(), null);
        }
        return response.result().stream().map(TelegramApi.Update::toDomain).toList();
    }

    @Override
    public void send(long chatId, Reply reply) {
        var request = TelegramApi.SendMessageRequest.of(chatId, reply);
        call("sendMessage", () -> http.post().uri("/sendMessage").body(request).retrieve().toBodilessEntity());
    }

    private static <T> T call(String method, Supplier<T> request) {
        try {
            return request.get();
        } catch (HttpStatusCodeException e) {
            throw new TelegramUnavailableException(
                    "%s failed with HTTP %d".formatted(method, e.getStatusCode().value()), retryAfter(e), null);
        } catch (RestClientException e) {
            throw new TelegramUnavailableException(
                    "%s failed: %s".formatted(method, e.getClass().getSimpleName()), Optional.empty(), null);
        }
    }

    private static Optional<Duration> retryAfter(HttpStatusCodeException e) {
        try {
            return Optional.ofNullable(e.getResponseBodyAs(TelegramApi.Response.class))
                    .map(TelegramApi.Response::parameters)
                    .map(TelegramApi.Parameters::retryAfter)
                    .map(Duration::ofSeconds);
        } catch (RuntimeException unreadableBody) {
            return Optional.empty();
        }
    }
}
