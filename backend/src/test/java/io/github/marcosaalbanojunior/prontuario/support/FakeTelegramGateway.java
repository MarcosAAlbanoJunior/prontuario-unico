package io.github.marcosaalbanojunior.prontuario.support;

import io.github.marcosaalbanojunior.prontuario.application.port.out.telegram.TelegramGatewayPort;
import io.github.marcosaalbanojunior.prontuario.application.port.out.telegram.TelegramUnavailableException;
import io.github.marcosaalbanojunior.prontuario.domain.model.telegram.Reply;
import io.github.marcosaalbanojunior.prontuario.domain.model.telegram.TelegramUpdate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class FakeTelegramGateway implements TelegramGatewayPort {

    public record Sent(long chatId, Reply reply) {}

    private final List<TelegramUpdate> pending = new ArrayList<>();
    private final List<Long> requestedOffsets = new ArrayList<>();
    private final List<Sent> sent = new ArrayList<>();
    private boolean sendFails;

    public void receive(TelegramUpdate... updates) {
        pending.addAll(List.of(updates));
    }

    public void failSending() {
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

    public List<Sent> sent() {
        return List.copyOf(sent);
    }

    public List<Long> requestedOffsets() {
        return List.copyOf(requestedOffsets);
    }
}
