package io.github.marcosaalbanojunior.prontuario.application.port.out;

import io.github.marcosaalbanojunior.prontuario.domain.model.goal.Goal;
import java.util.List;

public interface GoalRepositoryPort {

    List<Goal> activeGoals();
}
