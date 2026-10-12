package io.github.marcosaalbanojunior.prontuario.goals.application.service;

import io.github.marcosaalbanojunior.prontuario.goals.application.port.in.GetGoalProgress;
import io.github.marcosaalbanojunior.prontuario.goals.application.port.in.GoalProgressView;
import io.github.marcosaalbanojunior.prontuario.goals.application.port.out.ConfirmedTotals;
import io.github.marcosaalbanojunior.prontuario.goals.application.port.out.GoalRepository;
import io.github.marcosaalbanojunior.prontuario.goals.domain.Goal;
import io.github.marcosaalbanojunior.prontuario.goals.domain.GoalProgress;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

public final class GetGoalProgressService implements GetGoalProgress {

    private final GoalRepository goals;
    private final ConfirmedTotals totals;
    private final Clock clock;
    private final ZoneId zone;

    public GetGoalProgressService(GoalRepository goals, ConfirmedTotals totals, Clock clock, ZoneId zone) {
        this.goals = goals;
        this.totals = totals;
        this.clock = clock;
        this.zone = zone;
    }

    @Override
    public List<GoalProgressView> currentProgress() {
        var today = LocalDate.now(clock.withZone(zone));
        return goals.activeGoals().stream()
                .filter(Goal::isMeasurable)
                .map(goal -> progressOf(goal, today))
                .map(GetGoalProgressService::view)
                .toList();
    }

    private GoalProgress progressOf(Goal goal, LocalDate today) {
        var window = goal.period().windowContaining(today, zone);
        return new GoalProgress(goal, totals.total(goal.type(), window));
    }

    private static GoalProgressView view(GoalProgress progress) {
        var goal = progress.goal();
        return new GoalProgressView(code(goal.type()), code(goal.period()), goal.target(), progress.achieved(), goal.unit(),
                progress.percent(), progress.isMet());
    }

    private static String code(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }
}
