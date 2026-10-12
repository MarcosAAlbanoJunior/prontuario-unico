package io.github.marcosaalbanojunior.prontuario.application.usecase.entry;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.marcosaalbanojunior.prontuario.application.port.in.entry.UndoResult;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.CreatedVia;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.Event;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.EventDetails;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.EventStatus;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.EventType;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.TimePrecision;
import io.github.marcosaalbanojunior.prontuario.support.DirectTransactions;
import io.github.marcosaalbanojunior.prontuario.support.InMemoryEventRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UndoLastEntryService.undoLast()")
class UndoLastEntryServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-12T13:00:00Z");

    private final InMemoryEventRepository events = new InMemoryEventRepository();
    private final UndoLastEntryService service = new UndoLastEntryService(events, new DirectTransactions());

    @Test
    @DisplayName("discards the most recently recorded event only")
    void should_discardNewest_when_severalEventsExist() {
        var older = event(NOW.minusSeconds(60));
        var newer = event(NOW);
        events.add(older);
        events.add(newer);

        var result = service.undoLast();

        assertThat(result).isInstanceOf(UndoResult.Undone.class);
        assertThat(((UndoResult.Undone) result).event().id()).isEqualTo(newer.id());
        assertThat(statusOf(newer)).isEqualTo(EventStatus.DISCARDED);
        assertThat(statusOf(older)).isEqualTo(EventStatus.CONFIRMED);
    }

    @Test
    @DisplayName("undoes one event per call until nothing is left")
    void should_undoOnePerCall_when_calledRepeatedly() {
        var older = event(NOW.minusSeconds(60));
        events.add(older);
        events.add(event(NOW));

        service.undoLast();
        service.undoLast();

        assertThat(statusOf(older)).isEqualTo(EventStatus.DISCARDED);
        assertThat(service.undoLast()).isInstanceOf(UndoResult.NothingToUndo.class);
    }

    @Test
    @DisplayName("reports nothing to undo when there are no events")
    void should_returnNothingToUndo_when_noEventsExist() {
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
