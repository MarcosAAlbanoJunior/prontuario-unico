package io.github.marcosaalbanojunior.prontuario.entries.application.port.in;

public sealed interface QuickEntryResult {

    record Recorded(EntrySummary entry) implements QuickEntryResult {}

    record NeedsMore(MissingField missing) implements QuickEntryResult {}

    record Rejected(RejectionCause cause) implements QuickEntryResult {}

    /** The same chat message was already recorded; nothing changed. */
    record AlreadyRecorded() implements QuickEntryResult {}
}
