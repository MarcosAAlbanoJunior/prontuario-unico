package io.github.marcosaalbanojunior.prontuario.domain.service.quickentry;

import static io.github.marcosaalbanojunior.prontuario.domain.model.quickentry.ParseOutcome.NeedsMore;
import static io.github.marcosaalbanojunior.prontuario.domain.model.quickentry.ParseOutcome.Parsed;
import static io.github.marcosaalbanojunior.prontuario.domain.model.quickentry.ParseOutcome.Rejected;

import io.github.marcosaalbanojunior.prontuario.domain.model.quickentry.MissingInfo;
import io.github.marcosaalbanojunior.prontuario.domain.model.quickentry.ParseOutcome;
import io.github.marcosaalbanojunior.prontuario.domain.model.quickentry.QuickEntry;
import io.github.marcosaalbanojunior.prontuario.domain.model.quickentry.RejectionReason;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/** An optional activity followed by a duration: "musculação 50", "corrida 1h", "45". */
final class ExerciseArgs implements ArgsParser {

    static final Duration MAX_DURATION = Duration.ofHours(12);

    @Override
    public ParseOutcome parse(String args) {
        var tokens = Text.tokens(args);
        if (tokens.isEmpty()) {
            return new NeedsMore(MissingInfo.EXERCISE_DURATION);
        }
        var duration = Durations.parse(tokens.getLast(), ChronoUnit.MINUTES);
        if (duration.isEmpty()) {
            return new NeedsMore(MissingInfo.EXERCISE_DURATION);
        }
        if (!duration.get().isPositive() || duration.get().compareTo(MAX_DURATION) > 0) {
            return new Rejected(RejectionReason.OUT_OF_RANGE);
        }
        var activity = String.join(" ", tokens.subList(0, tokens.size() - 1));
        return new Parsed(new QuickEntry.Exercise(Optional.of(activity).filter(text -> !text.isEmpty()), duration.get()));
    }
}
