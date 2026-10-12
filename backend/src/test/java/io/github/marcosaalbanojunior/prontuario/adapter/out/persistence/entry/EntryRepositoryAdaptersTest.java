package io.github.marcosaalbanojunior.prontuario.adapter.out.persistence.entry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.marcosaalbanojunior.prontuario.adapter.out.persistence.PostgresTest;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.BodyRegion;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.CreatedVia;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.Event;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.EventDetails;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.EventStatus;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.EventType;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.Intensity;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.MeasureUnit;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.Measurement;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.Source;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.SourceKind;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.TimePrecision;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

@PostgresTest
@DisplayName("EventRepositoryAdapter and SourceRepositoryAdapter")
class EntryRepositoryAdaptersTest {

    private static final Instant MORNING = Instant.parse("2026-10-12T11:00:00Z");

    private final JdbcClient jdbc;
    private final EventRepositoryAdapter events;
    private final SourceRepositoryAdapter sources;

    EntryRepositoryAdaptersTest(JdbcClient jdbc) {
        this.jdbc = jdbc;
        this.events = new EventRepositoryAdapter(jdbc);
        this.sources = new SourceRepositoryAdapter(jdbc);
    }

    @Nested
    @DisplayName("add(...) and latestNotDiscarded()")
    class AddAndRead {

        @Test
        @DisplayName("stores and reads back every field of an event")
        void should_roundTripEveryField_when_eventIsStored() {
            var event = Event.confirmed(UUID.randomUUID(), pain(MORNING), source(1L), CreatedVia.COMMAND, MORNING);

            events.add(event);

            assertThat(events.latestNotDiscarded()).contains(event);
        }

        @Test
        @DisplayName("reads an event without a measurement")
        void should_readWithoutMeasurement_when_valueIsAbsent() {
            var note = new EventDetails(EventType.NOTE, Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), MORNING, Optional.empty(), TimePrecision.DAY);
            var event = Event.confirmed(UUID.randomUUID(), note, source(null), CreatedVia.WEB, MORNING);

            events.add(event);

            assertThat(events.latestNotDiscarded()).contains(event);
        }

        @Test
        @DisplayName("skips discarded events when looking for the latest")
        void should_skipDiscarded_when_lookingForLatest() {
            var older = addWater(MORNING, 300);
            var newer = addWater(MORNING.plusSeconds(5), 500);

            events.updateStatus(newer.id(), EventStatus.DISCARDED);

            assertThat(events.latestNotDiscarded()).map(Event::id).contains(older.id());
        }

        @Test
        @DisplayName("fails loudly on a unit the application does not know")
        void should_fail_when_storedUnitIsUnknown() {
            var event = addWater(MORNING, 300);
            jdbc.sql("UPDATE event SET unit = 'gallon' WHERE id = ?").param(event.id()).update();

            assertThatThrownBy(events::latestNotDiscarded).hasMessage("unknown unit in database: gallon");
        }
    }

    @Nested
    @DisplayName("sumConfirmedValues(...)")
    class Sum {

        @Test
        @DisplayName("sums only confirmed events of the type inside the period")
        void should_sumConfirmedInPeriod_when_eventsVary() {
            addWater(MORNING, 500);
            addWater(MORNING.plusSeconds(3600), 250);
            addWater(MORNING.minusSeconds(86_400), 1000);
            events.updateStatus(addWater(MORNING.plusSeconds(7200), 700).id(), EventStatus.DISCARDED);

            var total = events.sumConfirmedValues(EventType.WATER, MORNING.minusSeconds(3600), MORNING.plusSeconds(86_000));

            assertThat(total).isEqualByComparingTo("750");
        }

        @Test
        @DisplayName("is zero when nothing matches")
        void should_beZero_when_nothingMatches() {
            assertThat(events.sumConfirmedValues(EventType.SLEEP, MORNING, MORNING.plusSeconds(60))).isEqualByComparingTo("0");
        }
    }

    @Nested
    @DisplayName("SourceRepositoryAdapter.existsByTelegramMessageId(...)")
    class Sources {

        @Test
        @DisplayName("finds a source by its Telegram message id")
        void should_findSource_when_messageIdWasStored() {
            source(77L);

            assertThat(sources.existsByTelegramMessageId(77L)).isTrue();
            assertThat(sources.existsByTelegramMessageId(78L)).isFalse();
        }
    }

    private Event addWater(Instant at, int milliliters) {
        var details = new EventDetails(EventType.WATER, Optional.empty(), Optional.empty(),
                Optional.of(new Measurement(BigDecimal.valueOf(milliliters), MeasureUnit.MILLILITER)), Optional.empty(),
                at, Optional.empty(), TimePrecision.EXACT);
        var event = Event.confirmed(UUID.randomUUID(), details, source(null), CreatedVia.COMMAND, at);
        events.add(event);
        return event;
    }

    private UUID source(Long telegramMessageId) {
        var source = new Source(UUID.randomUUID(), SourceKind.TELEGRAM_COMMAND, Optional.of("/agua"),
                Optional.ofNullable(telegramMessageId), MORNING);
        sources.add(source);
        return source.id();
    }

    private static EventDetails pain(Instant at) {
        return new EventDetails(EventType.SYMPTOM, Optional.of("dor"), Optional.of(BodyRegion.RIGHT_KNEE), Optional.empty(),
                Optional.of(new Intensity(4)), at, Optional.of(at.plusSeconds(600)), TimePrecision.APPROX);
    }
}
