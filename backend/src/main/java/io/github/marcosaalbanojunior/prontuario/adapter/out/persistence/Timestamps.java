package io.github.marcosaalbanojunior.prontuario.adapter.out.persistence;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** The PostgreSQL driver binds timestamptz as OffsetDateTime, not Instant. */
public final class Timestamps {

    private Timestamps() {}

    public static OffsetDateTime toColumn(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    public static Instant fromColumn(OffsetDateTime value) {
        return value.toInstant();
    }
}
