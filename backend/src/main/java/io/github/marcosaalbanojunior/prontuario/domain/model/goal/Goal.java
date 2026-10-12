package io.github.marcosaalbanojunior.prontuario.domain.model.goal;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/** A target the user sets for themself; the unit matches how events are stored (ml, min). */
public record Goal(UUID id, GoalType type, BigDecimal target, String unit, GoalPeriod period, boolean active) {

    public Goal {
        Objects.requireNonNull(id);
        Objects.requireNonNull(type);
        Objects.requireNonNull(target);
        Objects.requireNonNull(unit);
        Objects.requireNonNull(period);
        if (target.signum() <= 0) {
            throw new InvalidGoalException("goal target must be positive");
        }
    }
}
