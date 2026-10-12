package io.github.marcosaalbanojunior.prontuario.support;

import io.github.marcosaalbanojunior.prontuario.application.port.out.IdGeneratorPort;
import java.util.UUID;

/** Predictable ids for tests: 00000000-0000-7000-8000-000000000001, ...0002, ... */
public final class SequentialIdGenerator implements IdGeneratorPort {

    private long next = 1;

    @Override
    public UUID newId() {
        return new UUID(0x7000L, 0x8000_0000_0000_0000L | next++);
    }
}
