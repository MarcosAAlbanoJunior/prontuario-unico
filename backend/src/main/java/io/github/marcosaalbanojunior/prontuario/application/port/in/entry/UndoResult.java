package io.github.marcosaalbanojunior.prontuario.application.port.in.entry;

import io.github.marcosaalbanojunior.prontuario.domain.model.entry.Event;

public sealed interface UndoResult {

    record Undone(Event event) implements UndoResult {}

    record NothingToUndo() implements UndoResult {}
}
