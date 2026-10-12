package io.github.marcosaalbanojunior.prontuario.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.marcosaalbanojunior.prontuario.application.port.out.EventRepositoryPort;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.Event;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.EventStatus;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.EventType;
import io.github.marcosaalbanojunior.prontuario.domain.model.goal.Goal;
import io.github.marcosaalbanojunior.prontuario.domain.model.goal.GoalPeriod;
import io.github.marcosaalbanojunior.prontuario.domain.model.goal.GoalProgress;
import io.github.marcosaalbanojunior.prontuario.domain.model.goal.GoalType;
import io.github.marcosaalbanojunior.prontuario.domain.model.goal.PeriodWindow;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GetGoalProgressService.currentProgress()")
class GetGoalProgressServiceTest {

    // Already the 14th in UTC, but still Tuesday the 13th at 23:30 in São Paulo: "today" is the local day.
    private static final Instant NOW = Instant.parse("2026-10-14T02:30:00Z");

    private final List<PeriodWindow> requestedWindows = new ArrayList<>();

    @Test
    @DisplayName("sums confirmed events in the current local day or week of each goal")
    void should_sumInLocalPeriod_when_goalsAreMeasurable() {
        var water = goal(GoalType.WATER, 2000, "ml", GoalPeriod.DAY);
        var exercise = goal(GoalType.EXERCISE, 150, "min", GoalPeriod.WEEK);

        var progress = service(List.of(water, exercise)).currentProgress();

        assertThat(progress).containsExactly(
                new GoalProgress(water, BigDecimal.valueOf(1500)),
                new GoalProgress(exercise, BigDecimal.valueOf(160)));
        assertThat(requestedWindows).containsExactly(
                new PeriodWindow(Instant.parse("2026-10-13T03:00:00Z"), Instant.parse("2026-10-14T03:00:00Z")),
                new PeriodWindow(Instant.parse("2026-10-12T03:00:00Z"), Instant.parse("2026-10-19T03:00:00Z")));
    }

    @Test
    @DisplayName("skips custom goals because nothing measures them yet")
    void should_skipCustomGoals_when_listingProgress() {
        assertThat(service(List.of(goal(GoalType.CUSTOM, 1, "x", GoalPeriod.DAY))).currentProgress()).isEmpty();
    }

    private GetGoalProgressService service(List<Goal> goals) {
        return new GetGoalProgressService(() -> goals, new TotalsOnly(), Clock.fixed(NOW, ZoneOffset.UTC),
                ZoneId.of("America/Sao_Paulo"));
    }

    private static Goal goal(GoalType type, int target, String unit, GoalPeriod period) {
        return new Goal(UUID.randomUUID(), type, BigDecimal.valueOf(target), unit, period, true);
    }

    /** Answers sums only: 1500 ml of water, 160 min of exercise. */
    private final class TotalsOnly implements EventRepositoryPort {

        @Override
        public BigDecimal sumConfirmedValues(EventType type, Instant from, Instant to) {
            requestedWindows.add(new PeriodWindow(from, to));
            return type == EventType.WATER ? BigDecimal.valueOf(1500) : BigDecimal.valueOf(160);
        }

        @Override
        public void add(Event event) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateStatus(UUID eventId, EventStatus status) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Event> latestNotDiscarded() {
            throw new UnsupportedOperationException();
        }
    }
}
