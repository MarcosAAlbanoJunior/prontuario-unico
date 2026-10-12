package io.github.marcosaalbanojunior.prontuario.telegram.application.port.out;

/** Remembers the last handled update so nothing is processed twice after a restart. */
public interface UpdateOffsetStore {

    /** The offset for the next getUpdates call: last handled update id + 1, or 0 on first run. */
    long nextOffset();

    void markHandled(long updateId);
}
