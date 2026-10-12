package io.github.marcosaalbanojunior.prontuario.telegram.application.port.out;

import io.github.marcosaalbanojunior.prontuario.telegram.domain.Reply;
import io.github.marcosaalbanojunior.prontuario.telegram.domain.TelegramUpdate;
import java.time.Duration;
import java.util.List;

public interface TelegramGateway {

    /** Blocks up to {@code longPoll} waiting for updates with id >= offset. */
    List<TelegramUpdate> fetchUpdates(long offset, Duration longPoll);

    void send(long chatId, Reply reply);
}
