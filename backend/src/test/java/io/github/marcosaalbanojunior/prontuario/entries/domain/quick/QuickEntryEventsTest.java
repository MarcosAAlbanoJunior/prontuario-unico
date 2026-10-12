package io.github.marcosaalbanojunior.prontuario.entries.domain.quick;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.marcosaalbanojunior.prontuario.entries.domain.BodyRegion;
import io.github.marcosaalbanojunior.prontuario.entries.domain.EventType;
import io.github.marcosaalbanojunior.prontuario.entries.domain.Intensity;
import io.github.marcosaalbanojunior.prontuario.entries.domain.MeasureUnit;
import io.github.marcosaalbanojunior.prontuario.entries.domain.Measurement;
import io.github.marcosaalbanojunior.prontuario.entries.domain.TimePrecision;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class QuickEntryEventsTest {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final ZonedDateTime NOW = ZonedDateTime.of(2026, 10, 12, 8, 15, 42, 0, SAO_PAULO);

    @Test
    void shouldRecordWaterNowInMilliliters() {
        var details = QuickEntryEvents.toDetails(new QuickEntry.Water(new BigDecimal("500")), NOW);

        assertThat(details.type()).isEqualTo(EventType.WATER);
        assertThat(details.measurement()).contains(new Measurement(new BigDecimal("500"), MeasureUnit.MILLILITER));
        assertThat(details.occurredStart()).isEqualTo(NOW.toInstant());
        assertThat(details.timePrecision()).isEqualTo(TimePrecision.EXACT);
    }

    @Test
    void shouldPlaceSleepDurationAsEndingNowWithApproximatePrecision() {
        var details = QuickEntryEvents.toDetails(new QuickEntry.SleepDuration(Duration.ofMinutes(400)), NOW);

        assertThat(details.occurredEnd()).contains(at(12, 8, 15));
        assertThat(details.occurredStart()).isEqualTo(at(12, 1, 35));
        assertThat(details.measurement()).contains(new Measurement(BigDecimal.valueOf(400), MeasureUnit.MINUTE));
        assertThat(details.timePrecision()).isEqualTo(TimePrecision.APPROX);
    }

    @ParameterizedTest(name = "slept {0} to {1} at 08:15 -> {2} {3} to {4} {5} ({6} min)")
    @CsvSource({
        "23:30, 06:10, 11, 23:30, 12, 06:10, 400",
        "01:00, 07:00, 12, 01:00, 12, 07:00, 360",
        "22:00, 09:00, 10, 22:00, 11, 09:00, 660",
        "14:00, 15:30, 11, 14:00, 11, 15:30, 90",
    })
    void shouldPlaceSleepSpanBeforeNow(String bedtime, String wakeUp, int startDay, String startTime,
                                        int endDay, String endTime, long minutes) {
        var span = new QuickEntry.SleepSpan(LocalTime.parse(bedtime), LocalTime.parse(wakeUp));

        var details = QuickEntryEvents.toDetails(span, NOW);

        assertThat(details.occurredStart()).isEqualTo(at(startDay, LocalTime.parse(startTime)));
        assertThat(details.occurredEnd()).contains(at(endDay, LocalTime.parse(endTime)));
        assertThat(details.measurement()).contains(new Measurement(BigDecimal.valueOf(minutes), MeasureUnit.MINUTE));
        assertThat(details.timePrecision()).isEqualTo(TimePrecision.EXACT);
    }

    @Test
    void shouldKeepExerciseActivityAsConcept() {
        var exercise = new QuickEntry.Exercise(Optional.of("musculação"), Duration.ofMinutes(50));

        var details = QuickEntryEvents.toDetails(exercise, NOW);

        assertThat(details.concept()).contains("musculação");
        assertThat(details.occurredStart()).isEqualTo(at(12, 7, 25));
    }

    @Test
    void shouldRecordPainAsSymptomInTheRegionWithIntensity() {
        var pain = new QuickEntry.Pain(BodyRegion.RIGHT_KNEE, Optional.of(new Intensity(4)));

        var details = QuickEntryEvents.toDetails(pain, NOW);

        assertThat(details.type()).isEqualTo(EventType.SYMPTOM);
        assertThat(details.concept()).contains("dor");
        assertThat(details.bodyRegion()).contains(BodyRegion.RIGHT_KNEE);
        assertThat(details.intensity()).contains(new Intensity(4));
    }

    private static java.time.Instant at(int day, int hour, int minute) {
        return at(day, LocalTime.of(hour, minute));
    }

    private static java.time.Instant at(int day, LocalTime time) {
        return ZonedDateTime.of(NOW.toLocalDate().withDayOfMonth(day), time, SAO_PAULO).toInstant();
    }
}
