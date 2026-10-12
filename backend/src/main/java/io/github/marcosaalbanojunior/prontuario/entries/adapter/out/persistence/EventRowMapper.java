package io.github.marcosaalbanojunior.prontuario.entries.adapter.out.persistence;

import io.github.marcosaalbanojunior.prontuario.entries.domain.BodyRegion;
import io.github.marcosaalbanojunior.prontuario.entries.domain.CreatedVia;
import io.github.marcosaalbanojunior.prontuario.entries.domain.DedupeKey;
import io.github.marcosaalbanojunior.prontuario.entries.domain.Event;
import io.github.marcosaalbanojunior.prontuario.entries.domain.EventDetails;
import io.github.marcosaalbanojunior.prontuario.entries.domain.EventStatus;
import io.github.marcosaalbanojunior.prontuario.entries.domain.EventType;
import io.github.marcosaalbanojunior.prontuario.entries.domain.Intensity;
import io.github.marcosaalbanojunior.prontuario.entries.domain.Measurement;
import io.github.marcosaalbanojunior.prontuario.entries.domain.TimePrecision;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;

/** Explicit mapping, no reflection, so it behaves the same in the native image. */
final class EventRowMapper implements RowMapper<Event> {

    @Override
    public Event mapRow(ResultSet row, int rowNumber) throws SQLException {
        var details = new EventDetails(
                Codes.parse(EventType.class, row.getString("type")),
                Optional.ofNullable(row.getString("concept")),
                Optional.ofNullable(row.getString("body_region")).map(code -> Codes.parse(BodyRegion.class, code)),
                measurement(row),
                Optional.ofNullable(row.getObject("intensity", Integer.class)).map(Intensity::new),
                instant(row, "occurred_start"),
                Optional.ofNullable(row.getObject("occurred_end", OffsetDateTime.class)).map(Timestamps::fromColumn),
                Codes.parse(TimePrecision.class, row.getString("time_precision")));
        return new Event(
                row.getObject("id", UUID.class),
                details,
                instant(row, "recorded_at"),
                Codes.parse(EventStatus.class, row.getString("status")),
                row.getObject("source_id", UUID.class),
                Codes.parse(CreatedVia.class, row.getString("created_via")),
                new DedupeKey(row.getString("dedupe_key")));
    }

    private static Optional<Measurement> measurement(ResultSet row) throws SQLException {
        BigDecimal value = row.getBigDecimal("value_num");
        String unit = row.getString("unit");
        if (value == null || unit == null) {
            return Optional.empty();
        }
        return Optional.of(new Measurement(value, MeasureUnits.fromSymbol(unit)));
    }

    private static Instant instant(ResultSet row, String column) throws SQLException {
        return Timestamps.fromColumn(row.getObject(column, OffsetDateTime.class));
    }
}
