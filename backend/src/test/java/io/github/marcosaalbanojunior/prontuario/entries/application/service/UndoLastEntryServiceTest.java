package io.github.marcosaalbanojunior.prontuario.entries.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.marcosaalbanojunior.prontuario.common.tx.DirectTransactionRunner;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.UndoResult;
import io.github.marcosaalbanojunior.prontuario.entries.domain.CreatedVia;
import io.github.marcosaalbanojunior.prontuario.entries.domain.Event;
import io.github.marcosaalbanojunior.prontuario.entries.domain.EventDetails;
import io.github.marcosaalbanojunior.prontuario.entries.domain.EventStatus;
import io.github.marcosaalbanojunior.prontuario.entries.domain.EventType;
import io.github.marcosaalbanojunior.prontuario.entries.domain.TimePrecision;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UndoLastEntryServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-12T13:00:00Z");

    private final InMemoryEventRepository events = new InMemoryEventRepository();
    private final UndoLastEntryService service = new UndoLastEntryService(events, new DirectTransactionRunner());

    @Test
    void shouldDiscardTheMostRecentlyRecordedEvent() {
        var older = event(NOW.minusSeconds(60));
        var newer = event(NOW);
        events.add(older);
        events.add(newer);

        var result = service.undoLast();

        assertThat(result).isInstanceOf(UndoResult.Undone.class);
        assertThat(((UndoResult.Undone) result).entry().eventId()).isEqualTo(newer.id());
        assertThat(statusOf(newer)).isEqualTo(EventStatus.DISCARDED);
        assertThat(statusOf(older)).isEqualTo(EventStatus.CONFIRMED);
    }

    @Test
    void shouldUndoOneEventPerCall() {
        var older = event(NOW.minusSeconds(60));
        events.add(older);
        events.add(event(NOW));

        service.undoLast();
        service.undoLast();

        assertThat(statusOf(older)).isEqualTo(EventStatus.DISCARDED);
        assertThat(service.undoLast()).isInstanceOf(UndoResult.NothingToUndo.class);
    }

    @Test
    void shouldReportNothingToUndoWhenThereAreNoEvents() {
        assertThat(service.undoLast()).isInstanceOf(UndoResult.NothingToUndo.class);
    }

    private EventStatus statusOf(Event event) {
        return events.all().stream().filter(stored -> stored.id().equals(event.id())).findFirst().orElseThrow().status();
    }

    private static Event event(Instant recordedAt) {
        var details = new EventDetails(EventType.NOTE, Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), recordedAt, Optional.empty(), TimePrecision.EXACT);
        return Event.confirmed(UUID.randomUUID(), details, UUID.randomUUID(), CreatedVia.COMMAND, recordedAt);
    }
}
