package io.github.marcosaalbanojunior.prontuario.domain.model.entry;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** A symptom or habit on the timeline. Only confirmed events count as facts. */
public record Event(
        UUID id,
        EventDetails details,
        Instant recordedAt,
        EventStatus status,
        UUID sourceId,
        CreatedVia createdVia,
        DedupeKey dedupeKey) {

    public Event {
        Objects.requireNonNull(id);
        Objects.requireNonNull(details);
        Objects.requireNonNull(recordedAt);
        Objects.requireNonNull(status);
        Objects.requireNonNull(sourceId);
        Objects.requireNonNull(createdVia);
        Objects.requireNonNull(dedupeKey);
    }

    public static Event confirmed(UUID id, EventDetails details, UUID sourceId, CreatedVia createdVia, Instant recordedAt) {
        return new Event(id, details, recordedAt, EventStatus.CONFIRMED, sourceId, createdVia, DedupeKey.of(details, sourceId));
    }

    public Event discard() {
        if (status == EventStatus.DISCARDED) {
            throw new InvalidEventException("event is already discarded");
        }
        return new Event(id, details, recordedAt, EventStatus.DISCARDED, sourceId, createdVia, dedupeKey);
    }
}
