package io.github.marcosaalbanojunior.prontuario.entries.adapter.out.persistence;

import io.github.marcosaalbanojunior.prontuario.entries.domain.MeasureUnit;
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
