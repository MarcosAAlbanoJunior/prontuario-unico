package io.github.marcosaalbanojunior.prontuario.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

@PostgresTest
@DisplayName("UpdateOffsetRepositoryAdapter")
class UpdateOffsetRepositoryAdapterTest {

    private final UpdateOffsetRepositoryAdapter offsets;

    UpdateOffsetRepositoryAdapterTest(JdbcClient jdbc) {
        this.offsets = new UpdateOffsetRepositoryAdapter(jdbc);
    }

    @Test
    @DisplayName("starts from zero")
    void should_startFromZero_when_nothingWasHandled() {
        assertThat(offsets.nextOffset()).isZero();
    }

    @Test
    @DisplayName("continues after the last handled update")
    void should_continueAfterLast_when_updatesWereHandled() {
        offsets.markHandled(100);
        offsets.markHandled(101);

        assertThat(offsets.nextOffset()).isEqualTo(102);
    }

    @Test
    @DisplayName("never moves backwards")
    void should_keepHighest_when_olderUpdateIsMarked() {
        offsets.markHandled(101);
        offsets.markHandled(100);

        assertThat(offsets.nextOffset()).isEqualTo(102);
    }
}
