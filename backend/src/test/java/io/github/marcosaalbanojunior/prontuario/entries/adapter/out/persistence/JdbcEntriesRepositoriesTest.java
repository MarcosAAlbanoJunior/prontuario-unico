package io.github.marcosaalbanojunior.prontuario.entries.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.marcosaalbanojunior.prontuario.TestcontainersConfiguration;
import io.github.marcosaalbanojunior.prontuario.entries.domain.BodyRegion;
import io.github.marcosaalbanojunior.prontuario.entries.domain.CreatedVia;
import io.github.marcosaalbanojunior.prontuario.entries.domain.Event;
import io.github.marcosaalbanojunior.prontuario.entries.domain.EventDetails;
import io.github.marcosaalbanojunior.prontuario.entries.domain.EventStatus;
import io.github.marcosaalbanojunior.prontuario.entries.domain.EventType;
import io.github.marcosaalbanojunior.prontuario.entries.domain.Intensity;
import io.github.marcosaalbanojunior.prontuario.entries.domain.MeasureUnit;
import io.github.marcosaalbanojunior.prontuario.entries.domain.Measurement;
import io.github.marcosaalbanojunior.prontuario.entries.domain.Source;
import io.github.marcosaalbanojunior.prontuario.entries.domain.SourceKind;
import io.github.marcosaalbanojunior.prontuario.entries.domain.TimePrecision;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

@DisabledInNativeImage
@JdbcTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class JdbcEntriesRepositoriesTest {

    private static final Instant MORNING = Instant.parse("2026-10-12T11:00:00Z");

    private final JdbcEventRepository events;
    private final JdbcSourceRepository sources;

    JdbcEntriesRepositoriesTest(JdbcClient jdbc) {
        this.events = new JdbcEventRepository(jdbc);
        this.sources = new JdbcSourceRepository(jdbc);
    }

    @Test
    void shouldStoreAndReadBackEveryEventField() {
        var event = Event.confirmed(UUID.randomUUID(), painDetails(MORNING), source(1L), CreatedVia.COMMAND, MORNING);

        events.add(event);

        assertThat(events.latestNotDiscarded()).contains(event);
    }

    @Test
    void shouldSkipDiscardedEventsWhenLookingForTheLatest() {
        var older = Event.confirmed(UUID.randomUUID(), water(MORNING, 300), source(1L), CreatedVia.COMMAND, MORNING);
        var newer = Event.confirmed(UUID.randomUUID(), water(MORNING, 500), source(2L), CreatedVia.COMMAND, MORNING.plusSeconds(5));
        events.add(older);
        events.add(newer);

        events.updateStatus(newer.id(), EventStatus.DISCARDED);

        assertThat(events.latestNotDiscarded()).map(Event::id).contains(older.id());
    }

    @Test
    void shouldSumOnlyConfirmedEventsOfTheTypeInsideThePeriod() {
        addWater(MORNING, 500);
        addWater(MORNING.plusSeconds(3600), 250);
        addWater(MORNING.minusSeconds(86_400), 1000);
        var discarded = addWater(MORNING.plusSeconds(7200), 700);
        events.updateStatus(discarded.id(), EventStatus.DISCARDED);

        var total = events.sumConfirmedValues(EventType.WATER, MORNING.minusSeconds(3600), MORNING.plusSeconds(86_000));

        assertThat(total).isEqualByComparingTo("750");
    }

    @Test
    void shouldFindSourcesByTelegramMessageId() {
        source(77L);

        assertThat(sources.existsByTelegramMessageId(77L)).isTrue();
        assertThat(sources.existsByTelegramMessageId(78L)).isFalse();
    }

    private Event addWater(Instant at, int milliliters) {
        var event = Event.confirmed(UUID.randomUUID(), water(at, milliliters), source(null), CreatedVia.COMMAND, at);
        events.add(event);
        return event;
    }

    private UUID source(Long telegramMessageId) {
        var source = new Source(UUID.randomUUID(), SourceKind.TELEGRAM_COMMAND, Optional.of("/agua"),
                Optional.ofNullable(telegramMessageId), MORNING);
        sources.add(source);
        return source.id();
    }

    private static EventDetails water(Instant at, int milliliters) {
        return new EventDetails(EventType.WATER, Optional.empty(), Optional.empty(),
                Optional.of(new Measurement(BigDecimal.valueOf(milliliters), MeasureUnit.MILLILITER)), Optional.empty(),
                at, Optional.empty(), TimePrecision.EXACT);
    }

    private static EventDetails painDetails(Instant at) {
        return new EventDetails(EventType.SYMPTOM, Optional.of("dor"), Optional.of(BodyRegion.RIGHT_KNEE), Optional.empty(),
                Optional.of(new Intensity(4)), at, Optional.of(at.plusSeconds(600)), TimePrecision.APPROX);
    }
}
