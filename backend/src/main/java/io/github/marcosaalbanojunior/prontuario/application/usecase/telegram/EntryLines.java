package io.github.marcosaalbanojunior.prontuario.application.usecase.telegram;

import io.github.marcosaalbanojunior.prontuario.application.port.out.MessagesPort;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.Event;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.EventDetails;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.Measurement;
import lombok.RequiredArgsConstructor;

/** One pt-BR line describing a recorded event, e.g. "Água: 500 mL às 14:32". */
@RequiredArgsConstructor
final class EntryLines {

    private final MessagesPort messages;
    private final PtBrFormat format;

    String describe(Event event) {
        var details = event.details();
        var start = format.time(details.occurredStart());
        var end = details.occurredEnd().map(format::time).orElse(start);
        return switch (details.type()) {
            case WATER, WEIGHT -> messages.get("entry." + MessageKeys.of(details.type()), quantity(details), start);
            case SLEEP -> messages.get("entry.sleep", quantity(details), start, end);
            case EXERCISE -> details.concept()
                    .map(activity -> messages.get("entry.exercise-with-activity", activity, quantity(details), end))
                    .orElseGet(() -> messages.get("entry.exercise", quantity(details), end));
            case SYMPTOM -> symptom(details, start);
            case MEAL, MEDICATION, NOTE -> messages.get("entry.other", start);
        };
    }

    private String symptom(EventDetails details, String start) {
        var region = details.bodyRegion().map(code -> messages.get("body-region." + MessageKeys.of(code))).orElse("");
        return details.intensity()
                .map(intensity -> messages.get("entry.pain-with-intensity", region, intensity.value(), start))
                .orElseGet(() -> messages.get("entry.pain", region, start));
    }

    private String quantity(EventDetails details) {
        return details.measurement().map(this::quantity).orElse("");
    }

    private String quantity(Measurement measurement) {
        return format.quantity(measurement.value(), measurement.unit().symbol());
    }
}
