package io.github.marcosaalbanojunior.prontuario.adapter.out.id;

import io.github.marcosaalbanojunior.prontuario.application.port.out.IdGeneratorPort;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import java.util.random.RandomGenerator;

/**
 * UUID version 7 (RFC 9562): 48-bit Unix epoch milliseconds followed by random bits,
 * so ids sort by creation time. The JDK only generates version 4.
 */
public final class UuidV7IdGeneratorAdapter implements IdGeneratorPort {

    private static final long VERSION_7 = 0x7L << 12;
    private static final long RFC_VARIANT = 0x8000_0000_0000_0000L;
    private static final long TIMESTAMP_MASK = 0xFFFF_FFFF_FFFFL;
    private static final long RANDOM_A_MASK = 0x0FFFL;
    private static final long RANDOM_B_MASK = 0x3FFF_FFFF_FFFF_FFFFL;

    private final Clock clock;
    private final RandomGenerator random;

    public UuidV7IdGeneratorAdapter(Clock clock, RandomGenerator random) {
        this.clock = Objects.requireNonNull(clock);
        this.random = Objects.requireNonNull(random);
    }

    @Override
    public UUID newId() {
        long timestamp = clock.millis() & TIMESTAMP_MASK;
        long mostSignificant = (timestamp << 16) | VERSION_7 | (random.nextLong() & RANDOM_A_MASK);
        long leastSignificant = RFC_VARIANT | (random.nextLong() & RANDOM_B_MASK);
        return new UUID(mostSignificant, leastSignificant);
    }
}
