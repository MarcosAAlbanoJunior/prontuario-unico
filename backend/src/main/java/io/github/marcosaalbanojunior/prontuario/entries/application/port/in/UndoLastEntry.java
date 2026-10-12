package io.github.marcosaalbanojunior.prontuario.entries.application.port.in;

/** Discards the most recently recorded event that is not discarded yet. */
public interface UndoLastEntry {

    UndoResult undoLast();
}
