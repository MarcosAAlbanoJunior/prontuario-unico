package io.github.marcosaalbanojunior.prontuario.domain.model.telegram;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A command waiting for missing data ("Quantos mL de água?"). The next plain text is appended
 * to {@code partialArgs} and the command is parsed again.
 */
public record PendingQuestion(String command, String partialArgs, Instant askedAt) {

    public static final Duration LIFETIME = Duration.ofMinutes(10);

    public PendingQuestion {
        Objects.requireNonNull(command);
        Objects.requireNonNull(partialArgs);
        Objects.requireNonNull(askedAt);
    }

    public boolean isExpiredAt(Instant now) {
        return !now.isBefore(askedAt.plus(LIFETIME));
    }

    public String argsWithAnswer(String answer) {
        return (partialArgs + " " + answer).strip();
    }
}
