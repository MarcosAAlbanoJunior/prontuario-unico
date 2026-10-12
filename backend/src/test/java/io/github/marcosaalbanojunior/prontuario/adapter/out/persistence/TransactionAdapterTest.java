package io.github.marcosaalbanojunior.prontuario.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.marcosaalbanojunior.prontuario.application.port.out.TransactionPort;
import io.github.marcosaalbanojunior.prontuario.support.TestcontainersConfiguration;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

@DisabledInNativeImage
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DisplayName("TransactionAdapter")
class TransactionAdapterTest {

    private final TransactionPort transactions;
    private final JdbcClient jdbc;

    TransactionAdapterTest(TransactionPort transactions, JdbcClient jdbc) {
        this.transactions = transactions;
        this.jdbc = jdbc;
    }

    @Test
    @DisplayName("commits when the work succeeds")
    void should_commit_when_workSucceeds() {
        transactions.inTransaction(() -> insertSource("commit"));

        assertThat(countSources("commit")).isEqualTo(1);
    }

    @Test
    @DisplayName("rolls everything back when the work fails")
    void should_rollBack_when_workFails() {
        assertThatThrownBy(() -> transactions.inTransaction(() -> {
            insertSource("rollback");
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(countSources("rollback")).isZero();
    }

    private void insertSource(String text) {
        jdbc.sql("INSERT INTO source (kind, raw_text, received_at) VALUES ('web', ?, ?)").params(text, OffsetDateTime.now()).update();
    }

    private long countSources(String text) {
        return jdbc.sql("SELECT count(*) FROM source WHERE raw_text = ?").param(text).query(Long.class).single();
    }
}
