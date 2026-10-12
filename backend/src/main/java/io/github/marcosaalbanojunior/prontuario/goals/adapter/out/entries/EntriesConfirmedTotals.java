package io.github.marcosaalbanojunior.prontuario.goals.adapter.out.entries;

import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.SumConfirmedEvents;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.TotalKind;
import io.github.marcosaalbanojunior.prontuario.goals.application.port.out.ConfirmedTotals;
import io.github.marcosaalbanojunior.prontuario.goals.domain.GoalType;
import io.github.marcosaalbanojunior.prontuario.goals.domain.PeriodWindow;
import java.math.BigDecimal;

/** Reads totals through the entries module's inbound port; goals never query events directly. */
public final class EntriesConfirmedTotals implements ConfirmedTotals {

    private final SumConfirmedEvents entries;

    public EntriesConfirmedTotals(SumConfirmedEvents entries) {
        this.entries = entries;
    }

    @Override
    public BigDecimal total(GoalType type, PeriodWindow window) {
        var kind = switch (type) {
            case WATER -> TotalKind.WATER_MILLILITERS;
            case SLEEP -> TotalKind.SLEEP_MINUTES;
            case EXERCISE -> TotalKind.EXERCISE_MINUTES;
            case CUSTOM -> throw new IllegalArgumentException("custom goals have no recorded total");
        };
        return entries.total(kind, window.from(), window.to());
    }
}
