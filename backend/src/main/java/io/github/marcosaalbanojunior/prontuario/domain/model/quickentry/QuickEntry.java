package io.github.marcosaalbanojunior.prontuario.domain.model.quickentry;

import io.github.marcosaalbanojunior.prontuario.domain.model.entry.BodyRegion;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.Intensity;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Optional;

/** The meaning of a parsed quick command, before it is placed in time. */
public sealed interface QuickEntry {

    record Water(BigDecimal milliliters) implements QuickEntry {}

    record SleepDuration(Duration duration) implements QuickEntry {}

    record SleepSpan(LocalTime start, LocalTime end) implements QuickEntry {}

    record Exercise(Optional<String> activity, Duration duration) implements QuickEntry {}

    record Weight(BigDecimal kilograms) implements QuickEntry {}

    record Pain(BodyRegion region, Optional<Intensity> intensity) implements QuickEntry {}
}
