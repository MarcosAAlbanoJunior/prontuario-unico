package io.github.marcosaalbanojunior.prontuario.common.id;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidV7GeneratorTest {

    private static final Instant NOW = Instant.parse("2026-10-12T08:00:00.123Z");

    @Test
    void shouldGenerateVersion7WithRfcVariant() {
        var id = generatorAt(NOW).newId();

        assertThat(id.version()).isEqualTo(7);
        assertThat(id.variant()).isEqualTo(2);
    }

    @Test
    void shouldEncodeClockMillisecondsInTheFirst48Bits() {
        var id = generatorAt(NOW).newId();

        assertThat(id.getMostSignificantBits() >>> 16).isEqualTo(NOW.toEpochMilli());
    }

    @Test
    void shouldSortByCreationTime() {
        var earlier = generatorAt(NOW).newId();
        var later = generatorAt(NOW.plusMillis(1)).newId();

        assertThat(earlier.toString()).isLessThan(later.toString());
    }

    @Test
    void shouldNotRepeatWithinTheSameMillisecond() {
        var generator = new UuidV7Generator(Clock.fixed(NOW, ZoneOffset.UTC), new SecureRandom());
        var ids = new HashSet<UUID>();

        for (int i = 0; i < 10_000; i++) {
            ids.add(generator.newId());
        }

        assertThat(ids).hasSize(10_000);
    }

    private static UuidV7Generator generatorAt(Instant instant) {
        return new UuidV7Generator(Clock.fixed(instant, ZoneOffset.UTC), new Random(42));
    }
}
