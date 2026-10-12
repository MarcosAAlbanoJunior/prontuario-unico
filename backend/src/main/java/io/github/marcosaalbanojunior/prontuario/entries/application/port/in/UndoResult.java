package io.github.marcosaalbanojunior.prontuario.entries.application.port.in;

public sealed interface UndoResult {

    record Undone(EntrySummary entry) implements UndoResult {}

    record NothingToUndo() implements UndoResult {}
}
