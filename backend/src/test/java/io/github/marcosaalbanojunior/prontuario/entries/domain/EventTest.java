package io.github.marcosaalbanojunior.prontuario.entries.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EventTest {

    private static final Instant START = Instant.parse("2026-10-12T11:00:30Z");
    private static final UUID SOURCE = UUID.fromString("0199d6a0-0000-7000-8000-000000000001");

    @Test
    void shouldRejectEventEndingBeforeStart() {
        assertThatThrownBy(() -> details(START, Optional.of(START.minusSeconds(60))))
                .isInstanceOf(InvalidEventException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 11})
    void shouldRejectIntensityOutsideZeroToTen(int value) {
        assertThatThrownBy(() -> new Intensity(value)).isInstanceOf(InvalidEventException.class);
    }

    @Test
    void shouldCreateConfirmedEventWithDedupeKey() {
        var event = Event.confirmed(UUID.randomUUID(), details(START, Optional.empty()), SOURCE, CreatedVia.COMMAND, START);

        assertThat(event.status()).isEqualTo(EventStatus.CONFIRMED);
        assertThat(event.dedupeKey().value()).hasSize(64);
    }

    @Test
    void shouldGiveSameDedupeKeyWithinTheSameMinuteAndSource() {
        var first = DedupeKey.of(details(START, Optional.empty()), SOURCE);
        var sameMinute = DedupeKey.of(details(START.plusSeconds(20), Optional.empty()), SOURCE);
        var otherSource = DedupeKey.of(details(START, Optional.empty()), UUID.randomUUID());

        assertThat(sameMinute).isEqualTo(first);
        assertThat(otherSource).isNotEqualTo(first);
    }

    @Test
    void shouldDiscardOnlyOnce() {
        var event = Event.confirmed(UUID.randomUUID(), details(START, Optional.empty()), SOURCE, CreatedVia.COMMAND, START);

        var discarded = event.discard();

        assertThat(discarded.status()).isEqualTo(EventStatus.DISCARDED);
        assertThatThrownBy(discarded::discard).isInstanceOf(InvalidEventException.class);
    }

    private static EventDetails details(Instant start, Optional<Instant> end) {
        return new EventDetails(EventType.SYMPTOM, Optional.of("dor"), Optional.of(BodyRegion.HEAD), Optional.empty(),
                Optional.of(new Intensity(3)), start, end, TimePrecision.EXACT);
    }
}
