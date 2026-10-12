package io.github.marcosaalbanojunior.prontuario.entries.domain.quick;

/** Essential data a command lacks; the user is asked for it instead of getting an error. */
public enum MissingInfo {
    WATER_AMOUNT,
    SLEEP_DURATION,
    EXERCISE_DURATION,
    WEIGHT_VALUE,
    BODY_REGION
}
