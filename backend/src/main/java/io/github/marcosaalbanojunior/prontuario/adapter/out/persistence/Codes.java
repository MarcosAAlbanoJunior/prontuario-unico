package io.github.marcosaalbanojunior.prontuario.adapter.out.persistence;

import java.util.Locale;

/** Enums are stored as lower-case codes (CHECK constraints in V1). */
public final class Codes {

    private Codes() {}

    public static String of(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }

    public static <E extends Enum<E>> E parse(Class<E> type, String code) {
        return Enum.valueOf(type, code.toUpperCase(Locale.ROOT));
    }
}
