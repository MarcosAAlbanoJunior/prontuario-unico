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
import java.util.List;

/** A duration ("6h40", "7", "6:40") or bedtime and wake-up time ("23:30 06:10"). */
final class SleepArgs implements ArgsParser {

    static final Duration MAX_DURATION = Duration.ofHours(24);

    private static final Rejected NOT_UNDERSTOOD = new Rejected(RejectionReason.NOT_UNDERSTOOD);

    @Override
    public ParseOutcome parse(String args) {
        var tokens = Text.tokens(args);
        return switch (tokens.size()) {
            case 0 -> new NeedsMore(MissingInfo.SLEEP_DURATION);
            case 1 -> duration(tokens.getFirst());
            case 2 -> span(tokens);
            default -> NOT_UNDERSTOOD;
        };
    }

    private static ParseOutcome duration(String token) {
        return Durations.parse(token, ChronoUnit.HOURS)
                .<ParseOutcome>map(duration -> isPlausible(duration)
                        ? new Parsed(new QuickEntry.SleepDuration(duration))
                        : new Rejected(RejectionReason.OUT_OF_RANGE))
                .orElse(NOT_UNDERSTOOD);
    }

    private static ParseOutcome span(List<String> tokens) {
        var start = Durations.clockTime(tokens.get(0));
        var end = Durations.clockTime(tokens.get(1));
        if (start.isEmpty() || end.isEmpty()) {
            return NOT_UNDERSTOOD;
        }
        return new Parsed(new QuickEntry.SleepSpan(start.get(), end.get()));
    }

    private static boolean isPlausible(Duration duration) {
        return duration.isPositive() && duration.compareTo(MAX_DURATION) <= 0;
    }
}
