package io.github.marcosaalbanojunior.prontuario.domain.service.quickentry;

import static io.github.marcosaalbanojunior.prontuario.domain.model.quickentry.ParseOutcome.NeedsMore;
import static io.github.marcosaalbanojunior.prontuario.domain.model.quickentry.ParseOutcome.Parsed;
import static io.github.marcosaalbanojunior.prontuario.domain.model.quickentry.ParseOutcome.Rejected;

import io.github.marcosaalbanojunior.prontuario.domain.model.quickentry.MissingInfo;
import io.github.marcosaalbanojunior.prontuario.domain.model.quickentry.ParseOutcome;
import io.github.marcosaalbanojunior.prontuario.domain.model.quickentry.QuickEntry;
import io.github.marcosaalbanojunior.prontuario.domain.model.quickentry.RejectionReason;

/** Body weight in kilograms: "82,4", "82.4", "82.4kg". */
final class WeightArgs implements ArgsParser {

    static final int MIN_KILOGRAMS = 20;
    static final int MAX_KILOGRAMS = 400;

    @Override
    public ParseOutcome parse(String args) {
        var compact = Text.clean(args).replace(" ", "");
        if (compact.isEmpty()) {
            return new NeedsMore(MissingInfo.WEIGHT_VALUE);
        }
        var number = compact.endsWith("kg") ? compact.substring(0, compact.length() - 2) : compact;
        return Numbers.decimal(number)
                .<ParseOutcome>map(kilograms -> Numbers.within(kilograms, MIN_KILOGRAMS, MAX_KILOGRAMS)
                        ? new Parsed(new QuickEntry.Weight(kilograms))
                        : new Rejected(RejectionReason.OUT_OF_RANGE))
                .orElse(new Rejected(RejectionReason.NOT_UNDERSTOOD));
    }
}
