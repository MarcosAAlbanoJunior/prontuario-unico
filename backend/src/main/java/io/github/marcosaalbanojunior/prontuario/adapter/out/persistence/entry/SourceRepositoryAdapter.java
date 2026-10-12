package io.github.marcosaalbanojunior.prontuario.adapter.out.persistence.entry;

import io.github.marcosaalbanojunior.prontuario.adapter.out.persistence.Codes;
import io.github.marcosaalbanojunior.prontuario.adapter.out.persistence.Timestamps;
import io.github.marcosaalbanojunior.prontuario.application.port.out.SourceRepositoryPort;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.Source;
import java.util.HashMap;
import org.springframework.jdbc.core.simple.JdbcClient;

public final class SourceRepositoryAdapter implements SourceRepositoryPort {

    private final JdbcClient jdbc;

    public SourceRepositoryAdapter(JdbcClient jdbc) {
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
