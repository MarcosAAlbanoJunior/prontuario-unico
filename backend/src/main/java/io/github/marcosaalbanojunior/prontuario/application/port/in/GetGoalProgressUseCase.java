package io.github.marcosaalbanojunior.prontuario.application.port.in;

import io.github.marcosaalbanojunior.prontuario.domain.model.goal.GoalProgress;
import java.util.List;

/** Progress of every active, measurable goal in its current period (today or this week). */
public interface GetGoalProgressUseCase {

    List<GoalProgress> currentProgress();
}
