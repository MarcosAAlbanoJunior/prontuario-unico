package io.github.marcosaalbanojunior.prontuario.domain.model.goal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record GoalProgress(Goal goal, BigDecimal achieved) {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public GoalProgress {
        Objects.requireNonNull(goal);
        Objects.requireNonNull(achieved);
    }

    public boolean isMet() {
        return achieved.compareTo(goal.target()) >= 0;
    }

    /** Whole percentage, not capped: going past the goal shows above 100. */
    public int percent() {
        return achieved.multiply(HUNDRED).divide(goal.target(), 0, RoundingMode.DOWN).intValueExact();
    }
}
