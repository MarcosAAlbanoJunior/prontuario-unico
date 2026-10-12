package io.github.marcosaalbanojunior.prontuario.domain.model.telegram;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** A chat message; {@code text} is empty for photos, files and other non-text messages. */
public record IncomingMessage(long chatId, long messageId, Optional<String> text, Instant sentAt) {

    public IncomingMessage {
        Objects.requireNonNull(text);
        Objects.requireNonNull(sentAt);
    }
}
