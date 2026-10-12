package io.github.marcosaalbanojunior.prontuario.telegram.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.marcosaalbanojunior.prontuario.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

@DisabledInNativeImage
@JdbcTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class JdbcUpdateOffsetStoreTest {

    private final JdbcUpdateOffsetStore offsets;

    JdbcUpdateOffsetStoreTest(JdbcClient jdbc) {
        this.offsets = new JdbcUpdateOffsetStore(jdbc);
    }

    @Test
    void shouldStartFromZero() {
        assertThat(offsets.nextOffset()).isZero();
    }

    @Test
    void shouldContinueAfterTheLastHandledUpdate() {
        offsets.markHandled(100);
        offsets.markHandled(101);

        assertThat(offsets.nextOffset()).isEqualTo(102);
    }

    @Test
    void shouldNeverMoveBackwards() {
        offsets.markHandled(101);
        offsets.markHandled(100);

        assertThat(offsets.nextOffset()).isEqualTo(102);
    }
}
