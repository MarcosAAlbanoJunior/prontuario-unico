package io.github.marcosaalbanojunior.prontuario.telegram;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.marcosaalbanojunior.prontuario.TestcontainersConfiguration;
import io.github.marcosaalbanojunior.prontuario.telegram.application.port.in.PollUpdates;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** The whole path: Telegram update -> command -> confirmed event in PostgreSQL -> reply. */
@DisabledInNativeImage
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class TelegramEndToEndTest {

    private static final long OWNER = 1L;
    private static final long NOW = Instant.now().getEpochSecond();
    private static final FakeTelegramServer TELEGRAM = start();

    private final PollUpdates pollUpdates;
    private final JdbcClient jdbc;

    TelegramEndToEndTest(PollUpdates pollUpdates, JdbcClient jdbc) {
        this.pollUpdates = pollUpdates;
        this.jdbc = jdbc;
    }

    @DynamicPropertySource
    static void telegramApi(DynamicPropertyRegistry registry) {
        registry.add("telegram.api-url", TELEGRAM::url);
        registry.add("TELEGRAM_BOT_TOKEN", () -> FakeTelegramServer.TOKEN);
        registry.add("TELEGRAM_ALLOWED_CHAT_ID", () -> OWNER);
    }

    @BeforeEach
    void cleanState() {
        TELEGRAM.clear();
        jdbc.sql("DELETE FROM event").update();
        jdbc.sql("DELETE FROM source").update();
        jdbc.sql("DELETE FROM telegram_offset").update();
    }

    @AfterAll
    static void stopTelegram() {
        TELEGRAM.close();
    }

    @Test
    void shouldRecordWaterAsConfirmedEventAndReply() {
        TELEGRAM.queueMessage(1, OWNER, 10, NOW, "/agua 500");

        pollUpdates.pollOnce();

        var event = jdbc.sql("SELECT type, value_num, unit, status, created_via FROM event").query().singleRow();
        assertThat(event).containsEntry("type", "water").containsEntry("unit", "ml")
                .containsEntry("status", "confirmed").containsEntry("created_via", "command");
        assertThat(event.get("value_num").toString()).isEqualTo("500");
        assertThat(TELEGRAM.sentMessages()).singleElement().asString().contains("Registrado. Água: 500 mL");
    }

    @Test
    void shouldAskWhereThePainIsAndRecordTheAnswer() {
        TELEGRAM.queueMessage(1, OWNER, 10, NOW, "/dor");
        pollUpdates.pollOnce();
        TELEGRAM.queueMessage(2, OWNER, 11, NOW, "joelho direito 4");
        pollUpdates.pollOnce();

        var event = jdbc.sql("SELECT type, body_region, intensity FROM event").query().singleRow();
        assertThat(event).isEqualTo(Map.of("type", "symptom", "body_region", "right_knee", "intensity", 4));
        assertThat(TELEGRAM.sentMessages().getFirst()).contains("Onde é a dor?").contains("\"force_reply\":true");
    }

    @Test
    void shouldNotRecordTwiceWhenTheSameMessageIsDeliveredAgain() {
        TELEGRAM.queueMessage(1, OWNER, 10, NOW, "/agua 500");
        pollUpdates.pollOnce();
        jdbc.sql("DELETE FROM telegram_offset").update();

        TELEGRAM.queueMessage(1, OWNER, 10, NOW, "/agua 500");
        pollUpdates.pollOnce();

        assertThat(jdbc.sql("SELECT count(*) FROM event").query(Long.class).single()).isEqualTo(1);
        assertThat(TELEGRAM.sentMessages()).hasSize(1);
    }

    @Test
    void shouldShowProgressAndUndoTheLastEntry() {
        TELEGRAM.queueMessage(1, OWNER, 10, NOW, "/agua 500");
        TELEGRAM.queueMessage(2, OWNER, 11, NOW, "/hoje");
        TELEGRAM.queueMessage(3, OWNER, 12, NOW, "/desfazer");
        pollUpdates.pollOnce();

        assertThat(TELEGRAM.sentMessages().get(1)).contains("Água: 500 mL de 2.000 mL (25%)");
        assertThat(TELEGRAM.sentMessages().get(2)).contains("Desfeito. Água: 500 mL");
        assertThat(jdbc.sql("SELECT status FROM event").query(String.class).single()).isEqualTo("discarded");
    }

    @Test
    void shouldIgnoreMessagesFromOtherChats() {
        TELEGRAM.queueMessage(1, 999, 10, NOW, "/agua 500");

        pollUpdates.pollOnce();

        assertThat(jdbc.sql("SELECT count(*) FROM event").query(Long.class).single()).isZero();
        assertThat(TELEGRAM.sentMessages()).isEmpty();
        assertThat(jdbc.sql("SELECT last_update_id FROM telegram_offset").query(Long.class).single()).isEqualTo(1);
    }

    private static FakeTelegramServer start() {
        try {
            return new FakeTelegramServer();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
