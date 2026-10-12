package io.github.marcosaalbanojunior.prontuario.entries.domain.quick;

import io.github.marcosaalbanojunior.prontuario.entries.domain.EventDetails;
import io.github.marcosaalbanojunior.prontuario.entries.domain.EventType;
import io.github.marcosaalbanojunior.prontuario.entries.domain.MeasureUnit;
import io.github.marcosaalbanojunior.prontuario.entries.domain.Measurement;
import io.github.marcosaalbanojunior.prontuario.entries.domain.TimePrecision;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/** Places a quick entry in time, relative to "now" in the user's time zone. */
public final class QuickEntryEvents {

    static final String PAIN_CONCEPT = "dor";

    private QuickEntryEvents() {}

    public static EventDetails toDetails(QuickEntry entry, ZonedDateTime now) {
        return switch (entry) {
            case QuickEntry.Water water -> pointInTime(EventType.WATER, new Measurement(water.milliliters(), MeasureUnit.MILLILITER), now);
            case QuickEntry.Weight weight -> pointInTime(EventType.WEIGHT, new Measurement(weight.kilograms(), MeasureUnit.KILOGRAM), now);
            case QuickEntry.SleepDuration sleep -> endingNow(EventType.SLEEP, Optional.empty(), sleep.duration(), now);
            case QuickEntry.Exercise exercise -> endingNow(EventType.EXERCISE, exercise.activity(), exercise.duration(), now);
            case QuickEntry.SleepSpan span -> sleepSpan(span, now);
            case QuickEntry.Pain pain -> new EventDetails(EventType.SYMPTOM, Optional.of(PAIN_CONCEPT), Optional.of(pain.region()),
                    Optional.empty(), pain.intensity(), now.toInstant(), Optional.empty(), TimePrecision.EXACT);
        };
    }

    private static EventDetails pointInTime(EventType type, Measurement measurement, ZonedDateTime now) {
        return new EventDetails(type, Optional.empty(), Optional.empty(), Optional.of(measurement), Optional.empty(),
                now.toInstant(), Optional.empty(), TimePrecision.EXACT);
    }

    private static EventDetails endingNow(EventType type, Optional<String> concept, Duration duration, ZonedDateTime now) {
        var end = now.truncatedTo(ChronoUnit.MINUTES);
        return interval(type, concept, end.minus(duration), end, TimePrecision.APPROX);
    }

    /** Wake-up is the latest occurrence of that clock time not after now; bedtime is the latest before wake-up. */
    private static EventDetails sleepSpan(QuickEntry.SleepSpan span, ZonedDateTime now) {
        var end = now.with(span.end());
        if (end.isAfter(now)) {
            end = end.minusDays(1);
        }
        var start = end.with(span.start());
        if (!start.isBefore(end)) {
            start = start.minusDays(1);
        }
        return interval(EventType.SLEEP, Optional.empty(), start, end, TimePrecision.EXACT);
    }

    private static EventDetails interval(EventType type, Optional<String> concept, ZonedDateTime start, ZonedDateTime end,
                                         TimePrecision precision) {
        var minutes = new Measurement(BigDecimal.valueOf(Duration.between(start, end).toMinutes()), MeasureUnit.MINUTE);
        return new EventDetails(type, concept, Optional.empty(), Optional.of(minutes), Optional.empty(),
                start.toInstant(), Optional.of(end.toInstant()), precision);
    }
}
