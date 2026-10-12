package io.github.marcosaalbanojunior.prontuario.application.port.out.telegram;

import io.github.marcosaalbanojunior.prontuario.domain.model.telegram.Reply;
import io.github.marcosaalbanojunior.prontuario.domain.model.telegram.TelegramUpdate;
import java.time.Duration;
import java.util.List;

public interface TelegramGatewayPort {

    /** Blocks up to {@code longPoll} waiting for updates with id >= offset. */
    List<TelegramUpdate> fetchUpdates(long offset, Duration longPoll);

    void send(long chatId, Reply reply);
}
