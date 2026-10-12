package io.github.marcosaalbanojunior.prontuario.application.port.out.telegram;

import java.time.Duration;
import java.util.Optional;

/** Telegram could not be reached or refused the call; {@code retryAfter} is set when it asked us to wait. */
public class TelegramUnavailableException extends RuntimeException {

    private final transient Optional<Duration> retryAfter;

    public TelegramUnavailableException(String message, Optional<Duration> retryAfter, Throwable cause) {
        super(message, cause);
        this.retryAfter = retryAfter;
    }

    public Optional<Duration> retryAfter() {
        return retryAfter;
    }
}
