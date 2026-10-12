package io.github.marcosaalbanojunior.prontuario.support;

import io.github.marcosaalbanojunior.prontuario.application.port.out.EventRepositoryPort;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.Event;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.EventStatus;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.EventType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryEventRepository implements EventRepositoryPort {

    private final List<Event> events = new ArrayList<>();

    @Override
    public void add(Event event) {
        events.add(event);
    }

    @Override
    public void updateStatus(UUID eventId, EventStatus status) {
        events.replaceAll(event -> event.id().equals(eventId)
                ? new Event(event.id(), event.details(), event.recordedAt(), status, event.sourceId(), event.createdVia(), event.dedupeKey())
                : event);
    }

    @Override
    public Optional<Event> latestNotDiscarded() {
        return events.stream()
                .filter(event -> event.status() != EventStatus.DISCARDED)
                .max(Comparator.comparing(Event::recordedAt).thenComparing(Event::id));
    }

    @Override
    public BigDecimal sumConfirmedValues(EventType type, Instant from, Instant to) {
        throw new UnsupportedOperationException("covered by EntryRepositoryAdaptersTest against PostgreSQL");
    }

    public List<Event> all() {
        return List.copyOf(events);
    }
}
