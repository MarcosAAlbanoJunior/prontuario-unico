package io.github.marcosaalbanojunior.prontuario.domain.model.telegram;

import java.util.Objects;
import java.util.Optional;

/** One item from getUpdates. Updates without a message (edits, joins) still advance the offset. */
public record TelegramUpdate(long updateId, Optional<IncomingMessage> message) {

    public TelegramUpdate {
        Objects.requireNonNull(message);
    }
}
