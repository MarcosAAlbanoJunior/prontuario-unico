package io.github.marcosaalbanojunior.prontuario.telegram.application.service;

import io.github.marcosaalbanojunior.prontuario.telegram.application.port.out.TelegramGateway;
import io.github.marcosaalbanojunior.prontuario.telegram.application.port.out.TelegramUnavailableException;
import io.github.marcosaalbanojunior.prontuario.telegram.domain.Reply;
import io.github.marcosaalbanojunior.prontuario.telegram.domain.TelegramUpdate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class FakeTelegramGateway implements TelegramGateway {

    record Sent(long chatId, Reply reply) {}

    private final List<TelegramUpdate> pending = new ArrayList<>();
    private final List<Long> requestedOffsets = new ArrayList<>();
    private final List<Sent> sent = new ArrayList<>();
    private boolean sendFails;

    void receive(TelegramUpdate... updates) {
        pending.addAll(List.of(updates));
    }

    void failSending() {
        sendFails = true;
    }

    @Override
    public List<TelegramUpdate> fetchUpdates(long offset, Duration longPoll) {
        requestedOffsets.add(offset);
        var batch = pending.stream().filter(update -> update.updateId() >= offset).toList();
        pending.clear();
        return batch;
    }

    @Override
    public void send(long chatId, Reply reply) {
        if (sendFails) {
            throw new TelegramUnavailableException("sendMessage failed with HTTP 502", Optional.empty(), null);
        }
        sent.add(new Sent(chatId, reply));
    }

    List<Sent> sent() {
        return List.copyOf(sent);
    }

    List<Long> requestedOffsets() {
        return List.copyOf(requestedOffsets);
    }
}
