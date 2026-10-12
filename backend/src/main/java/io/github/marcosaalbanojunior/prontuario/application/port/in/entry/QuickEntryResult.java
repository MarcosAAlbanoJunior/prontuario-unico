package io.github.marcosaalbanojunior.prontuario.application.port.in.entry;

import io.github.marcosaalbanojunior.prontuario.domain.model.entry.Event;
import io.github.marcosaalbanojunior.prontuario.domain.model.quickentry.MissingInfo;
import io.github.marcosaalbanojunior.prontuario.domain.model.quickentry.RejectionReason;

public sealed interface QuickEntryResult {

    record Recorded(Event event) implements QuickEntryResult {}

    /** The command lacks essential data; ask the user for it. */
    record NeedsMore(MissingInfo missing) implements QuickEntryResult {}

    record Rejected(RejectionReason reason) implements QuickEntryResult {}

    /** The same chat message was already recorded; nothing changed. */
    record AlreadyRecorded() implements QuickEntryResult {}
}
