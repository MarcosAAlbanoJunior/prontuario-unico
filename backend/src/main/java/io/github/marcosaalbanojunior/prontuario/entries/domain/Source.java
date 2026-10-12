package io.github.marcosaalbanojunior.prontuario.entries.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** The raw origin of a record, kept so every fact can be traced back to it. */
public record Source(UUID id, SourceKind kind, Optional<String> rawText, Optional<Long> telegramMessageId, Instant receivedAt) {

    public Source {
        Objects.requireNonNull(id);
        Objects.requireNonNull(kind);
        Objects.requireNonNull(rawText);
        Objects.requireNonNull(telegramMessageId);
        Objects.requireNonNull(receivedAt);
    }
}
