package io.github.marcosaalbanojunior.prontuario.domain.model.entry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Event and its value objects")
class EventTest {

    private static final Instant START = Instant.parse("2026-10-12T11:00:30Z");
    private static final UUID SOURCE = UUID.fromString("0199d6a0-0000-7000-8000-000000000001");

    @Nested
    @DisplayName("EventDetails")
    class Details {

        @Test
        @DisplayName("rejects an event that ends before it starts")
        void should_reject_when_endIsBeforeStart() {
            assertThatThrownBy(() -> details(START, Optional.of(START.minusSeconds(60))))
                    .isInstanceOf(InvalidEventException.class)
                    .hasMessage("event must not end before it starts");
        }

        @Test
        @DisplayName("accepts an event that ends when it starts")
        void should_accept_when_endEqualsStart() {
            assertThat(details(START, Optional.of(START)).occurredEnd()).contains(START);
        }
    }

    @Nested
    @DisplayName("Intensity")
    class IntensityRange {

        @ParameterizedTest(name = "rejects intensity {0}")
        @ValueSource(ints = {-1, 11})
        @DisplayName("rejects values outside 0 to 10")
        void should_reject_when_outsideZeroToTen(int value) {
            assertThatThrownBy(() -> new Intensity(value)).isInstanceOf(InvalidEventException.class);
        }

        @ParameterizedTest(name = "accepts intensity {0}")
        @ValueSource(ints = {0, 5, 10})
        @DisplayName("accepts values from 0 to 10")
        void should_accept_when_insideZeroToTen(int value) {
            assertThat(new Intensity(value).value()).isEqualTo(value);
        }
    }

    @Nested
    @DisplayName("Measurement")
    class Measurements {

        @Test
        @DisplayName("rejects a negative value")
        void should_reject_when_valueIsNegative() {
            assertThatThrownBy(() -> new Measurement(new BigDecimal("-1"), MeasureUnit.MILLILITER))
                    .isInstanceOf(InvalidEventException.class);
        }

        @Test
        @DisplayName("accepts zero")
        void should_accept_when_valueIsZero() {
            assertThat(new Measurement(BigDecimal.ZERO, MeasureUnit.MINUTE).unit().symbol()).isEqualTo("min");
        }
    }

    @Nested
    @DisplayName("confirmed(...)")
    class Confirmed {

        @Test
        @DisplayName("creates a confirmed event with a dedupe key from its details and source")
        void should_createConfirmedEvent_when_called() {
            var details = details(START, Optional.empty());

            var event = Event.confirmed(UUID.randomUUID(), details, SOURCE, CreatedVia.COMMAND, START);

            assertThat(event.status()).isEqualTo(EventStatus.CONFIRMED);
            assertThat(event.dedupeKey()).isEqualTo(DedupeKey.of(details, SOURCE));
        }
    }

    @Nested
    @DisplayName("discard()")
    class Discard {

        @Test
        @DisplayName("marks the event as discarded and keeps everything else")
        void should_discard_when_notDiscardedYet() {
            var event = Event.confirmed(UUID.randomUUID(), details(START, Optional.empty()), SOURCE, CreatedVia.COMMAND, START);

            var discarded = event.discard();

            assertThat(discarded.status()).isEqualTo(EventStatus.DISCARDED);
            assertThat(discarded.details()).isEqualTo(event.details());
            assertThat(discarded.dedupeKey()).isEqualTo(event.dedupeKey());
        }

        @Test
        @DisplayName("refuses to discard twice")
        void should_reject_when_alreadyDiscarded() {
            var discarded = Event.confirmed(UUID.randomUUID(), details(START, Optional.empty()), SOURCE, CreatedVia.COMMAND, START)
                    .discard();

            assertThatThrownBy(discarded::discard).isInstanceOf(InvalidEventException.class);
        }
    }

    static EventDetails details(Instant start, Optional<Instant> end) {
        return new EventDetails(EventType.SYMPTOM, Optional.of("dor"), Optional.of(BodyRegion.HEAD), Optional.empty(),
                Optional.of(new Intensity(3)), start, end, TimePrecision.EXACT);
    }
}
