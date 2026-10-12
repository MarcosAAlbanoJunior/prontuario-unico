package io.github.marcosaalbanojunior.prontuario.entries.application.port.in;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Sums confirmed events whose end (or start, when they have no end) falls in [from, to).
 * Sleep therefore counts on the day you wake up.
 */
public interface SumConfirmedEvents {

    BigDecimal total(TotalKind kind, Instant from, Instant to);
}
