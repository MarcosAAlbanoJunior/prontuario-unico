package io.github.marcosaalbanojunior.prontuario.application.port.out;

import io.github.marcosaalbanojunior.prontuario.domain.model.entry.Event;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.EventStatus;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.EventType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface EventRepositoryPort {

    void add(Event event);

    void updateStatus(UUID eventId, EventStatus status);

    Optional<Event> latestNotDiscarded();

    /**
     * Sum of the values of confirmed events of a type whose end (or start, when they have no end)
     * falls in [from, to). Sleep therefore counts on the day you wake up.
     */
    BigDecimal sumConfirmedValues(EventType type, Instant from, Instant to);
}
