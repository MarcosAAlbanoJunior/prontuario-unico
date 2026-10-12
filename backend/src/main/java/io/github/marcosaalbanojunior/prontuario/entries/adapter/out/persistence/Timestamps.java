package io.github.marcosaalbanojunior.prontuario.entries.adapter.out.persistence;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** The PostgreSQL driver binds timestamptz as OffsetDateTime, not Instant. */
final class Timestamps {

    private Timestamps() {}

    static OffsetDateTime toColumn(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    static Instant fromColumn(OffsetDateTime value) {
        return value.toInstant();
    }
}
