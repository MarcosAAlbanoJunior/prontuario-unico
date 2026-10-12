package io.github.marcosaalbanojunior.prontuario.entries.application.port.out;

import io.github.marcosaalbanojunior.prontuario.entries.domain.Event;
import io.github.marcosaalbanojunior.prontuario.entries.domain.EventStatus;
import io.github.marcosaalbanojunior.prontuario.entries.domain.EventType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository {

    void add(Event event);

    void updateStatus(UUID eventId, EventStatus status);

    Optional<Event> latestNotDiscarded();

    BigDecimal sumConfirmedValues(EventType type, Instant from, Instant to);
}
