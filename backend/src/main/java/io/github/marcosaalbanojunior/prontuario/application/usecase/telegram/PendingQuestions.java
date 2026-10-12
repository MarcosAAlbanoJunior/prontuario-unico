package io.github.marcosaalbanojunior.prontuario.application.usecase.telegram;

import io.github.marcosaalbanojunior.prontuario.domain.model.telegram.PendingQuestion;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/** At most one open question: there is a single user and a new command replaces it. Lost on restart, by design. */
final class PendingQuestions {

    private final AtomicReference<PendingQuestion> current = new AtomicReference<>();

    void ask(PendingQuestion question) {
        current.set(question);
    }

    void clear() {
        current.set(null);
    }

    /** Removes and returns the open question, if it has not expired by {@code now}. */
    Optional<PendingQuestion> take(Instant now) {
        return Optional.ofNullable(current.getAndSet(null)).filter(question -> !question.isExpiredAt(now));
    }
}
