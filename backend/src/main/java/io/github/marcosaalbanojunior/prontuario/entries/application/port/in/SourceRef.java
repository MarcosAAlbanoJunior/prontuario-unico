package io.github.marcosaalbanojunior.prontuario.entries.application.port.in;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Where an entry came from; {@code receivedAt} is when the user sent it, used as "now" for the entry. */
public record SourceRef(SourceChannel channel, String rawText, Optional<Long> telegramMessageId, Instant receivedAt) {

    public SourceRef {
        Objects.requireNonNull(channel);
        Objects.requireNonNull(rawText);
        Objects.requireNonNull(telegramMessageId);
        Objects.requireNonNull(receivedAt);
    }
}
