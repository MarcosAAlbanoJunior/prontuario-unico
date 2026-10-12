package io.github.marcosaalbanojunior.prontuario.telegram.adapter.out.chat;

import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.EntrySummary;
import java.math.BigDecimal;

/** One pt-BR line describing a recorded entry, e.g. "Água: 500 mL às 14:32". */
final class EntryLines {

    private final Texts texts;
    private final PtBrFormat format;

    EntryLines(Texts texts, PtBrFormat format) {
        this.texts = texts;
        this.format = format;
    }

    String describe(EntrySummary entry) {
        var start = format.time(entry.occurredStart());
        var end = entry.occurredEnd().map(format::time).orElse(start);
        return switch (entry.type()) {
            case "water", "weight" -> texts.get("entry." + entry.type(), quantity(entry), start);
            case "sleep" -> texts.get("entry.sleep", quantity(entry), start, end);
            case "exercise" -> entry.concept()
                    .map(activity -> texts.get("entry.exercise.activity", activity, quantity(entry), end))
                    .orElseGet(() -> texts.get("entry.exercise", quantity(entry), end));
            case "symptom" -> symptom(entry, start);
            default -> texts.get("entry.other", start);
        };
    }

    private String symptom(EntrySummary entry, String start) {
        var region = entry.bodyRegion().map(code -> texts.get("body_region." + code)).orElse("");
        return entry.intensity()
                .map(intensity -> texts.get("entry.pain.intensity", region, intensity, start))
                .orElseGet(() -> texts.get("entry.pain", region, start));
    }

    private String quantity(EntrySummary entry) {
        return format.quantity(entry.value().orElse(BigDecimal.ZERO), entry.unit().orElse(""));
    }
}
