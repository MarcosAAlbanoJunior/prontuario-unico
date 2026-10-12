package io.github.marcosaalbanojunior.prontuario.application.port.out.telegram;

/** Remembers the last handled update so nothing is processed twice after a restart. */
public interface UpdateOffsetRepositoryPort {

    /** The offset for the next getUpdates call: last handled update id + 1, or 0 on first run. */
    long nextOffset();

    void markHandled(long updateId);
}
