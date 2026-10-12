package io.github.marcosaalbanojunior.prontuario.domain.model.goal;

import java.time.Instant;
import java.util.Objects;

/** Half-open interval [from, to). */
public record PeriodWindow(Instant from, Instant to) {

    public PeriodWindow {
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("period must start before it ends");
        }
    }
}
