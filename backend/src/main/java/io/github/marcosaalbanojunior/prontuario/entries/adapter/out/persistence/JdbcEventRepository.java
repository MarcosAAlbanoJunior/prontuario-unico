package io.github.marcosaalbanojunior.prontuario.entries.adapter.out.persistence;

import io.github.marcosaalbanojunior.prontuario.entries.application.port.out.EventRepository;
import io.github.marcosaalbanojunior.prontuario.entries.domain.Event;
import io.github.marcosaalbanojunior.prontuario.entries.domain.EventStatus;
import io.github.marcosaalbanojunior.prontuario.entries.domain.EventType;
import io.github.marcosaalbanojunior.prontuario.entries.domain.Intensity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

public final class JdbcEventRepository implements EventRepository {

    private static final EventRowMapper ROW_MAPPER = new EventRowMapper();

    private final JdbcClient jdbc;

    public JdbcEventRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void add(Event event) {
        jdbc.sql("""
                INSERT INTO event (id, type, concept, body_region, value_num, unit, intensity, occurred_start,
                                   occurred_end, time_precision, recorded_at, status, source_id, created_via, dedupe_key)
                VALUES (:id, :type, :concept, :bodyRegion, :value, :unit, :intensity, :occurredStart,
                        :occurredEnd, :timePrecision, :recordedAt, :status, :sourceId, :createdVia, :dedupeKey)
                """)
                .params(columns(event))
                .update();
    }

    @Override
    public void updateStatus(UUID eventId, EventStatus status) {
        jdbc.sql("UPDATE event SET status = :status WHERE id = :id")
                .param("status", Codes.of(status))
                .param("id", eventId)
                .update();
    }

    @Override
    public Optional<Event> latestNotDiscarded() {
        return jdbc.sql("SELECT * FROM event WHERE status <> 'discarded' ORDER BY recorded_at DESC, id DESC LIMIT 1")
                .query(ROW_MAPPER)
                .optional();
    }

    @Override
    public BigDecimal sumConfirmedValues(EventType type, Instant from, Instant to) {
        return jdbc.sql("""
                SELECT coalesce(sum(value_num), 0)
                  FROM event
                 WHERE status = 'confirmed'
                   AND type = :type
                   AND coalesce(occurred_end, occurred_start) >= :from
                   AND coalesce(occurred_end, occurred_start) < :to
                """)
                .param("type", Codes.of(type))
                .param("from", Timestamps.toColumn(from))
                .param("to", Timestamps.toColumn(to))
                .query(BigDecimal.class)
                .single();
    }

    private static Map<String, Object> columns(Event event) {
        var details = event.details();
        var columns = new HashMap<String, Object>();
        columns.put("id", event.id());
        columns.put("type", Codes.of(details.type()));
        columns.put("concept", details.concept().orElse(null));
        columns.put("bodyRegion", details.bodyRegion().map(Codes::of).orElse(null));
        columns.put("value", details.measurement().map(measurement -> measurement.value()).orElse(null));
        columns.put("unit", details.measurement().map(measurement -> measurement.unit().symbol()).orElse(null));
        columns.put("intensity", details.intensity().map(Intensity::value).orElse(null));
        columns.put("occurredStart", Timestamps.toColumn(details.occurredStart()));
        columns.put("occurredEnd", details.occurredEnd().map(Timestamps::toColumn).orElse(null));
        columns.put("timePrecision", Codes.of(details.timePrecision()));
        columns.put("recordedAt", Timestamps.toColumn(event.recordedAt()));
        columns.put("status", Codes.of(event.status()));
        columns.put("sourceId", event.sourceId());
        columns.put("createdVia", Codes.of(event.createdVia()));
        columns.put("dedupeKey", event.dedupeKey().value());
        return columns;
    }
}
