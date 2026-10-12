package io.github.marcosaalbanojunior.prontuario.application.usecase;

import io.github.marcosaalbanojunior.prontuario.application.port.in.GetGoalProgressUseCase;
import io.github.marcosaalbanojunior.prontuario.application.port.out.EventRepositoryPort;
import io.github.marcosaalbanojunior.prontuario.application.port.out.GoalRepositoryPort;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.EventType;
import io.github.marcosaalbanojunior.prontuario.domain.model.goal.Goal;
import io.github.marcosaalbanojunior.prontuario.domain.model.goal.GoalProgress;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class GetGoalProgressService implements GetGoalProgressUseCase {

    private final GoalRepositoryPort goals;
    private final EventRepositoryPort events;
    private final Clock clock;
    private final ZoneId zone;

    @Override
    public List<GoalProgress> currentProgress() {
        var today = LocalDate.now(clock.withZone(zone));
        // Custom goals are skipped: nothing measures them yet.
        return goals.activeGoals().stream()
                .flatMap(goal -> goal.type().measuredBy().map(eventType -> progressOf(goal, eventType, today)).stream())
                .toList();
    }

    private GoalProgress progressOf(Goal goal, EventType eventType, LocalDate today) {
        var window = goal.period().windowContaining(today, zone);
        return new GoalProgress(goal, events.sumConfirmedValues(eventType, window.from(), window.to()));
    }
}
