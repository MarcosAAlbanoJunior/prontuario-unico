package io.github.marcosaalbanojunior.prontuario.application.port.in.entry;

import io.github.marcosaalbanojunior.prontuario.domain.model.quickentry.QuickCommand;
import java.util.Objects;

public record QuickEntryRequest(QuickCommand command, String args, SourceRef source) {

    public QuickEntryRequest {
        Objects.requireNonNull(command);
        Objects.requireNonNull(args);
        Objects.requireNonNull(source);
    }
}
