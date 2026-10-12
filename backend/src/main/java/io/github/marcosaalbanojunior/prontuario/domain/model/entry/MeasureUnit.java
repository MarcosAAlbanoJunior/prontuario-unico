package io.github.marcosaalbanojunior.prontuario.domain.model.entry;

/** Canonical units: every measurement is normalized to one of these before it is stored. */
public enum MeasureUnit {
    MILLILITER("ml"),
    MINUTE("min"),
    KILOGRAM("kg");

    private final String symbol;

    MeasureUnit(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }
}
