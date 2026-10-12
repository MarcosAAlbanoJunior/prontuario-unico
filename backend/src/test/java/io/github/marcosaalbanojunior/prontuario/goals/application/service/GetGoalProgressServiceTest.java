package io.github.marcosaalbanojunior.prontuario.goals.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.marcosaalbanojunior.prontuario.goals.application.port.in.GoalProgressView;
import io.github.marcosaalbanojunior.prontuario.goals.application.port.out.ConfirmedTotals;
import io.github.marcosaalbanojunior.prontuario.goals.domain.Goal;
import io.github.marcosaalbanojunior.prontuario.goals.domain.GoalPeriod;
import io.github.marcosaalbanojunior.prontuario.goals.domain.GoalType;
import io.github.marcosaalbanojunior.prontuario.goals.domain.PeriodWindow;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetGoalProgressServiceTest {

    // Already the 14th in UTC, but still Tuesday the 13th at 23:30 in São Paulo: "today" is the local day.
    private static final Instant NOW = Instant.parse("2026-10-14T02:30:00Z");

    private final List<PeriodWindow> requestedWindows = new ArrayList<>();

    @Test
    void shouldComputeProgressInTheCurrentLocalPeriod() {
        var goals = List.of(
                goal(GoalType.WATER, 2000, "ml", GoalPeriod.DAY),
                goal(GoalType.EXERCISE, 150, "min", GoalPeriod.WEEK));
        ConfirmedTotals totals = (type, window) -> {
            requestedWindows.add(window);
            return type == GoalType.WATER ? BigDecimal.valueOf(1500) : BigDecimal.valueOf(160);
        };

        var progress = service(goals, totals).currentProgress();

        assertThat(progress).containsExactly(
                new GoalProgressView("water", "day", BigDecimal.valueOf(2000), BigDecimal.valueOf(1500), "ml", 75, false),
                new GoalProgressView("exercise", "week", BigDecimal.valueOf(150), BigDecimal.valueOf(160), "min", 106, true));
        assertThat(requestedWindows).containsExactly(
                new PeriodWindow(Instant.parse("2026-10-13T03:00:00Z"), Instant.parse("2026-10-14T03:00:00Z")),
                new PeriodWindow(Instant.parse("2026-10-12T03:00:00Z"), Instant.parse("2026-10-19T03:00:00Z")));
    }

    @Test
    void shouldSkipCustomGoalsBecauseNothingMeasuresThem() {
        var goals = List.of(goal(GoalType.CUSTOM, 1, "x", GoalPeriod.DAY));

        var progress = service(goals, (type, window) -> BigDecimal.ZERO).currentProgress();

        assertThat(progress).isEmpty();
    }

    private static GetGoalProgressService service(List<Goal> goals, ConfirmedTotals totals) {
        return new GetGoalProgressService(() -> goals, totals, Clock.fixed(NOW, ZoneOffset.UTC), ZoneId.of("America/Sao_Paulo"));
    }

    private static Goal goal(GoalType type, int target, String unit, GoalPeriod period) {
        return new Goal(UUID.randomUUID(), type, BigDecimal.valueOf(target), unit, period, true);
    }
}
