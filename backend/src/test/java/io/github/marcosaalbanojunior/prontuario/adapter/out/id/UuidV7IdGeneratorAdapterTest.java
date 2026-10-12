package io.github.marcosaalbanojunior.prontuario.adapter.out.id;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UuidV7IdGeneratorAdapter.newId()")
class UuidV7IdGeneratorAdapterTest {

    private static final Instant NOW = Instant.parse("2026-10-12T08:00:00.123Z");

    @Test
    @DisplayName("generates version 7 with the RFC variant")
    void should_beVersion7_when_generated() {
        var id = generatorAt(NOW).newId();

        assertThat(id.version()).isEqualTo(7);
        assertThat(id.variant()).isEqualTo(2);
    }

    @Test
    @DisplayName("encodes the clock milliseconds in the first 48 bits")
    void should_encodeMillis_when_generated() {
        assertThat(generatorAt(NOW).newId().getMostSignificantBits() >>> 16).isEqualTo(NOW.toEpochMilli());
    }

    @Test
    @DisplayName("sorts by creation time")
    void should_sortByTime_when_generatedLater() {
        var earlier = generatorAt(NOW).newId();
        var later = generatorAt(NOW.plusMillis(1)).newId();

        assertThat(earlier.toString()).isLessThan(later.toString());
    }

    @Test
    @DisplayName("does not repeat within the same millisecond")
    void should_beUnique_when_generatedInSameMillisecond() {
        var generator = new UuidV7IdGeneratorAdapter(Clock.fixed(NOW, ZoneOffset.UTC), new SecureRandom());
        var ids = new HashSet<UUID>();

        for (int i = 0; i < 10_000; i++) {
            ids.add(generator.newId());
        }

        assertThat(ids).hasSize(10_000);
    }

    private static UuidV7IdGeneratorAdapter generatorAt(Instant instant) {
        return new UuidV7IdGeneratorAdapter(Clock.fixed(instant, ZoneOffset.UTC), new Random(42));
    }
}
