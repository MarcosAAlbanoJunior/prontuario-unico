package io.github.marcosaalbanojunior.prontuario.entries.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record Measurement(BigDecimal value, MeasureUnit unit) {

    public Measurement {
        Objects.requireNonNull(value);
        Objects.requireNonNull(unit);
        if (value.signum() < 0) {
            throw new InvalidEventException("measurement must not be negative");
        }
    }
}
