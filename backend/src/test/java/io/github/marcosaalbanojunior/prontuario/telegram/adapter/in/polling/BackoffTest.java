package io.github.marcosaalbanojunior.prontuario.telegram.adapter.in.polling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class BackoffTest {

    @Test
    void shouldDoubleUpToSixtySecondsAndRestartAfterReset() {
        var backoff = new Backoff();

        var waits = IntStream.range(0, 8).mapToObj(i -> backoff.next().toSeconds()).toList();
        backoff.reset();

        assertThat(waits).containsExactly(1L, 2L, 4L, 8L, 16L, 32L, 60L, 60L);
        assertThat(backoff.next()).isEqualTo(Duration.ofSeconds(1));
    }
}
