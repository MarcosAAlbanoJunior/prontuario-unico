package io.github.marcosaalbanojunior.prontuario.entries.application.service;

import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.EntrySummary;
import io.github.marcosaalbanojunior.prontuario.entries.domain.Event;
import io.github.marcosaalbanojunior.prontuario.entries.domain.Intensity;
import io.github.marcosaalbanojunior.prontuario.entries.domain.Measurement;
import java.util.Locale;

final class EntrySummaries {

    private EntrySummaries() {}

    static EntrySummary of(Event event) {
        var details = event.details();
        return new EntrySummary(
                event.id(),
                code(details.type()),
                details.concept(),
                details.bodyRegion().map(EntrySummaries::code),
                details.measurement().map(Measurement::value),
                details.measurement().map(measurement -> measurement.unit().symbol()),
                details.intensity().map(Intensity::value),
                details.occurredStart(),
                details.occurredEnd());
    }

    private static String code(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }
}
