package io.github.marcosaalbanojunior.prontuario.goals.application.port.in;

import java.util.List;

/** Progress of every active, measurable goal in its current period (today or this week). */
public interface GetGoalProgress {

    List<GoalProgressView> currentProgress();
}
