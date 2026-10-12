package io.github.marcosaalbanojunior.prontuario.application.port.in.entry;

/** Discards the most recently recorded event that is not discarded yet. */
public interface UndoLastEntryUseCase {

    UndoResult undoLast();
}
