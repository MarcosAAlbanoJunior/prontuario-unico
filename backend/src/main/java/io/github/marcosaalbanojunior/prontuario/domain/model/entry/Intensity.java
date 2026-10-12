package io.github.marcosaalbanojunior.prontuario.domain.model.entry;

public record Intensity(int value) {

    public static final int MIN = 0;
    public static final int MAX = 10;

    public Intensity {
        if (value < MIN || value > MAX) {
            throw new InvalidEventException("intensity must be between 0 and 10");
        }
    }
}
