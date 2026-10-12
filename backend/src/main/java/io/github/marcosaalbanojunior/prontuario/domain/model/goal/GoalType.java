package io.github.marcosaalbanojunior.prontuario.domain.model.goal;

import io.github.marcosaalbanojunior.prontuario.domain.model.entry.EventType;
import java.util.Optional;

public enum GoalType {
    WATER,
    SLEEP,
    EXERCISE,
    CUSTOM;

    /** The confirmed events whose values add up to this goal; custom goals have none yet. */
    public Optional<EventType> measuredBy() {
        return switch (this) {
            case WATER -> Optional.of(EventType.WATER);
            case SLEEP -> Optional.of(EventType.SLEEP);
            case EXERCISE -> Optional.of(EventType.EXERCISE);
            case CUSTOM -> Optional.empty();
        };
    }
}
