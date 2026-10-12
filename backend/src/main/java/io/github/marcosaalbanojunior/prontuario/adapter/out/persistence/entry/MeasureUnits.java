package io.github.marcosaalbanojunior.prontuario.adapter.out.persistence.entry;

import io.github.marcosaalbanojunior.prontuario.domain.model.entry.MeasureUnit;
import java.util.Arrays;

final class MeasureUnits {

    private MeasureUnits() {}

    static MeasureUnit fromSymbol(String symbol) {
        return Arrays.stream(MeasureUnit.values())
                .filter(unit -> unit.symbol().equals(symbol))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("unknown unit in database: " + symbol));
    }
}
