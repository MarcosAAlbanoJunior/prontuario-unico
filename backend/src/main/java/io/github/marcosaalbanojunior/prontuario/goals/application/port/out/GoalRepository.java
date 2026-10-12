package io.github.marcosaalbanojunior.prontuario.goals.application.port.out;

import io.github.marcosaalbanojunior.prontuario.goals.domain.Goal;
import java.util.List;

public interface GoalRepository {

    List<Goal> activeGoals();
}
