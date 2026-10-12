package io.github.marcosaalbanojunior.prontuario.common.id;

import java.util.UUID;

/** Creates identifiers for new aggregates before they are persisted. */
public interface IdGenerator {

    UUID newId();
}
