package io.github.marcosaalbanojunior.prontuario.entries.application.port.in;

import java.util.Objects;

public record QuickEntryRequest(QuickEntryType type, String args, SourceRef source) {

    public QuickEntryRequest {
        Objects.requireNonNull(type);
        Objects.requireNonNull(args);
        Objects.requireNonNull(source);
    }
}
