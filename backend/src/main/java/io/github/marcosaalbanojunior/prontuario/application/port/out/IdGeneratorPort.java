package io.github.marcosaalbanojunior.prontuario.application.port.out;

import java.util.UUID;

/** Creates identifiers for new aggregates before they are persisted. */
public interface IdGeneratorPort {

    UUID newId();
}
