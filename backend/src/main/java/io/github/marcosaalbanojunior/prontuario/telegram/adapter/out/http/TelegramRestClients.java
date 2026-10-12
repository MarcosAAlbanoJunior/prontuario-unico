package io.github.marcosaalbanojunior.prontuario.telegram.adapter.out.http;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** A RestClient for the Bot API whose read timeout outlasts the long poll. */
public final class TelegramRestClients {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_MARGIN = Duration.ofSeconds(15);

    private TelegramRestClients() {}

    public static RestClient create(String apiUrl, String botToken, Duration longPoll) {
        var requestFactory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build());
        requestFactory.setReadTimeout(longPoll.plus(READ_MARGIN));
        return RestClient.builder()
                .baseUrl(apiUrl + "/bot" + botToken)
                .requestFactory(requestFactory)
                .build();
    }
}
