package io.github.marcosaalbanojunior.prontuario.entries.application.service;

import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.SumConfirmedEvents;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.TotalKind;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.out.EventRepository;
import io.github.marcosaalbanojunior.prontuario.entries.domain.EventType;
import java.math.BigDecimal;
import java.time.Instant;

public final class SumConfirmedEventsService implements SumConfirmedEvents {

    private final EventRepository events;

    public SumConfirmedEventsService(EventRepository events) {
        this.events = events;
    }

    @Override
    public BigDecimal total(TotalKind kind, Instant from, Instant to) {
        return events.sumConfirmedValues(eventType(kind), from, to);
    }

    private static EventType eventType(TotalKind kind) {
        return switch (kind) {
            case WATER_MILLILITERS -> EventType.WATER;
            case SLEEP_MINUTES -> EventType.SLEEP;
            case EXERCISE_MINUTES -> EventType.EXERCISE;
        };
    }
}
