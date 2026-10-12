package io.github.marcosaalbanojunior.prontuario.entries.adapter.out.persistence;

import io.github.marcosaalbanojunior.prontuario.entries.application.port.out.SourceRepository;
import io.github.marcosaalbanojunior.prontuario.entries.domain.Source;
import java.util.HashMap;
import org.springframework.jdbc.core.simple.JdbcClient;

public final class JdbcSourceRepository implements SourceRepository {

    private final JdbcClient jdbc;

    public JdbcSourceRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void add(Source source) {
        var columns = new HashMap<String, Object>();
        columns.put("id", source.id());
        columns.put("kind", Codes.of(source.kind()));
        columns.put("rawText", source.rawText().orElse(null));
        columns.put("telegramMessageId", source.telegramMessageId().orElse(null));
        columns.put("receivedAt", Timestamps.toColumn(source.receivedAt()));
        jdbc.sql("""
                INSERT INTO source (id, kind, raw_text, telegram_message_id, received_at)
                VALUES (:id, :kind, :rawText, :telegramMessageId, :receivedAt)
                """)
                .params(columns)
                .update();
    }

    @Override
    public boolean existsByTelegramMessageId(long telegramMessageId) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM source WHERE telegram_message_id = ?)")
                .param(telegramMessageId)
                .query(Boolean.class)
                .single();
    }
}
