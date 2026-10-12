package io.github.marcosaalbanojunior.prontuario.domain.model.entry;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** What happened and when, independent of how it was recorded. */
public record EventDetails(
        EventType type,
        Optional<String> concept,
        Optional<BodyRegion> bodyRegion,
        Optional<Measurement> measurement,
        Optional<Intensity> intensity,
        Instant occurredStart,
        Optional<Instant> occurredEnd,
        TimePrecision timePrecision) {

    public EventDetails {
        Objects.requireNonNull(type);
        Objects.requireNonNull(concept);
        Objects.requireNonNull(bodyRegion);
        Objects.requireNonNull(measurement);
        Objects.requireNonNull(intensity);
        Objects.requireNonNull(occurredStart);
        Objects.requireNonNull(occurredEnd);
        Objects.requireNonNull(timePrecision);
        if (occurredEnd.filter(end -> end.isBefore(occurredStart)).isPresent()) {
            throw new InvalidEventException("event must not end before it starts");
        }
    }
}
