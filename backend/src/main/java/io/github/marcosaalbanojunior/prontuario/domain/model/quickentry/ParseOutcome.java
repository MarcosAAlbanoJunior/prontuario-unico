package io.github.marcosaalbanojunior.prontuario.domain.model.quickentry;

public sealed interface ParseOutcome {

    record Parsed(QuickEntry entry) implements ParseOutcome {}

    record NeedsMore(MissingInfo missing) implements ParseOutcome {}

    record Rejected(RejectionReason reason) implements ParseOutcome {}
}
