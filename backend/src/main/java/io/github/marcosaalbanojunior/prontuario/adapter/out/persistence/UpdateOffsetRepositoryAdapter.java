package io.github.marcosaalbanojunior.prontuario.adapter.out.persistence;

import io.github.marcosaalbanojunior.prontuario.application.port.out.telegram.UpdateOffsetRepositoryPort;
import org.springframework.jdbc.core.simple.JdbcClient;

public final class UpdateOffsetRepositoryAdapter implements UpdateOffsetRepositoryPort {

    private final JdbcClient jdbc;

    public UpdateOffsetRepositoryAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long nextOffset() {
        return jdbc.sql("SELECT last_update_id + 1 FROM telegram_offset WHERE id = 1")
                .query(Long.class)
                .optional()
                .orElse(0L);
    }

    @Override
    public void markHandled(long updateId) {
        jdbc.sql("""
                INSERT INTO telegram_offset (id, last_update_id) VALUES (1, :updateId)
                ON CONFLICT (id) DO UPDATE SET last_update_id = greatest(telegram_offset.last_update_id, excluded.last_update_id)
                """)
                .param("updateId", updateId)
                .update();
    }
}
