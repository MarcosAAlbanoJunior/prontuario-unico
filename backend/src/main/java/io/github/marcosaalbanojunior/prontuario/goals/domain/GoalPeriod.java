package io.github.marcosaalbanojunior.prontuario.goals.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

public enum GoalPeriod {
    DAY,
    WEEK;

    /** The period containing {@code today}, in the user's time zone. Weeks start on Monday. */
    public PeriodWindow windowContaining(LocalDate today, ZoneId zone) {
        var first = switch (this) {
            case DAY -> today;
            case WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        };
        var next = switch (this) {
            case DAY -> first.plusDays(1);
            case WEEK -> first.plusWeeks(1);
        };
        return new PeriodWindow(first.atStartOfDay(zone).toInstant(), next.atStartOfDay(zone).toInstant());
    }
}
