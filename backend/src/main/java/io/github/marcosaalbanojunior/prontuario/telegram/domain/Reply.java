package io.github.marcosaalbanojunior.prontuario.telegram.domain;

import java.util.Objects;

/** A message to send back; {@code expectsAnswer} asks Telegram to open the reply field. */
public record Reply(String text, boolean expectsAnswer) {

    public Reply {
        Objects.requireNonNull(text);
    }

    public static Reply of(String text) {
        return new Reply(text, false);
    }

    public static Reply question(String text) {
        return new Reply(text, true);
    }
}
