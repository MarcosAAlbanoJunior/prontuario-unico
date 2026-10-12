package io.github.marcosaalbanojunior.prontuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

@DisabledInNativeImage
@JdbcTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class SchemaMigrationTest {

    private static final OffsetDateTime NOW = OffsetDateTime.of(2026, 10, 12, 8, 0, 0, 0, ZoneOffset.UTC);
    private static final String SHA256 = "a".repeat(64);

    private final JdbcClient jdbc;

    SchemaMigrationTest(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Test
    void shouldGenerateUuidV7IdsByDefault() {
        var id = insertSource();

        assertThat(id.version()).isEqualTo(7);
    }

    @Test
    void shouldAcceptValidEventAsProposed() {
        var id = insert("event", validEvent(insertSource()));

        var status = jdbc.sql("SELECT status FROM event WHERE id = ?").param(id).query(String.class).single();
        assertThat(status).isEqualTo("proposed");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidEvents")
    void shouldRejectInvalidEvent(String description, Map<String, Object> overrides) {
        var event = validEvent(insertSource());
        event.putAll(overrides);

        assertThatThrownBy(() -> insert("event", event)).isInstanceOf(DataIntegrityViolationException.class);
    }

    static Stream<Arguments> invalidEvents() {
        return Stream.of(
                Arguments.of("intensity above 10", Map.of("intensity", 11)),
                Arguments.of("negative intensity", Map.of("intensity", -1)),
                Arguments.of("end before start", Map.of("occurred_end", NOW.minusMinutes(1))),
                Arguments.of("unknown type", Map.of("type", "mood")),
                Arguments.of("unknown body region", Map.of("body_region", "right_ear")),
                Arguments.of("unknown status", Map.of("status", "maybe")),
                Arguments.of("unknown time precision", Map.of("time_precision", "week")),
                Arguments.of("unknown creation channel", Map.of("created_via", "email")));
    }

    @Test
    void shouldRejectDuplicateDedupeKey() {
        var sourceId = insertSource();
        insert("event", validEvent(sourceId));

        assertThatThrownBy(() -> insert("event", validEvent(sourceId))).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void shouldRejectSameDocumentTwice() {
        insert("document", validDocument());

        assertThatThrownBy(() -> insert("document", validDocument())).isInstanceOf(DuplicateKeyException.class);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidDocuments")
    void shouldRejectInvalidDocument(String description, Map<String, Object> overrides) {
        var document = validDocument();
        document.putAll(overrides);

        assertThatThrownBy(() -> insert("document", document)).isInstanceOf(DataIntegrityViolationException.class);
    }

    static Stream<Arguments> invalidDocuments() {
        return Stream.of(
                Arguments.of("larger than 20 MB", Map.of("size_bytes", 20_971_521L)),
                Arguments.of("empty file", Map.of("size_bytes", 0L)),
                Arguments.of("unsupported type", Map.of("mime", "application/zip")),
                Arguments.of("malformed sha256", Map.of("sha256", "not-a-hash")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidLabResults")
    void shouldRejectInvalidLabResult(String description, Map<String, Object> overrides) {
        var result = validLabResult(insertExam());
        result.putAll(overrides);

        assertThatThrownBy(() -> insert("lab_result", result)).isInstanceOf(DataIntegrityViolationException.class);
    }

    static Stream<Arguments> invalidLabResults() {
        var noValue = new HashMap<String, Object>();
        noValue.put("value_num", null);
        return Stream.of(
                Arguments.of("without any value", noValue),
                Arguments.of("reference range inverted", Map.of("ref_low", 50, "ref_high", 10)),
                Arguments.of("unknown status", Map.of("status", "critical")));
    }

    @Test
    void shouldRejectGuidanceLinkedToBothAnalyteAndConcept() {
        jdbc.sql("INSERT INTO analyte (code, display_name) VALUES ('alt', 'TGP')").update();
        var guidance = new HashMap<String, Object>(Map.of(
                "appointment_id", insertAppointment(),
                "text", "Refazer o exame",
                "analyte_code", "alt",
                "concept", "dor de cabeça"));

        assertThatThrownBy(() -> insert("guidance", guidance)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectGoalWithoutPositiveTarget() {
        var goal = Map.<String, Object>of("type", "water", "target_value", 0, "unit", "ml", "period", "day");

        assertThatThrownBy(() -> insert("goal", goal)).isInstanceOf(DataIntegrityViolationException.class);
    }

    private UUID insertSource() {
        return insert("source", Map.of("kind", "telegram_command", "raw_text", "/agua 500", "received_at", NOW));
    }

    private UUID insertExam() {
        return insert("exam", Map.of("collected_on", NOW.toLocalDate(), "source_id", insertSource(), "created_at", NOW));
    }

    private UUID insertAppointment() {
        return insert("appointment", Map.of("occurred_on", NOW.toLocalDate(), "source_id", insertSource()));
    }

    private static Map<String, Object> validEvent(UUID sourceId) {
        return new HashMap<>(Map.of(
                "type", "symptom",
                "concept", "dor de cabeça",
                "body_region", "head",
                "intensity", 4,
                "occurred_start", NOW,
                "time_precision", "approx",
                "recorded_at", NOW,
                "source_id", sourceId,
                "created_via", "free_text",
                "dedupe_key", "symptom|dor de cabeça|" + NOW + "|" + sourceId));
    }

    private static Map<String, Object> validDocument() {
        return new HashMap<>(Map.of(
                "filename", "hemograma.pdf",
                "mime", "application/pdf",
                "size_bytes", 1024L,
                "sha256", SHA256,
                "storage_path", "documents/" + SHA256,
                "uploaded_at", NOW));
    }

    private static Map<String, Object> validLabResult(UUID examId) {
        return new HashMap<>(Map.of(
                "exam_id", examId,
                "analyte_name_raw", "TGP",
                "value_num", 52,
                "unit", "U/L",
                "ref_high", 41,
                "status", "above"));
    }

    private UUID insert(String table, Map<String, Object> columns) {
        var names = String.join(", ", columns.keySet());
        var placeholders = columns.keySet().stream().map(name -> ":" + name).collect(Collectors.joining(", "));
        return jdbc.sql("INSERT INTO " + table + " (" + names + ") VALUES (" + placeholders + ") RETURNING id")
                .paramSource(columns)
                .query(UUID.class)
                .single();
    }
}
