package io.github.marcosaalbanojunior.prontuario.telegram.application.service;

import io.github.marcosaalbanojunior.prontuario.common.tx.TransactionRunner;
import io.github.marcosaalbanojunior.prontuario.telegram.application.port.in.PollUpdates;
import io.github.marcosaalbanojunior.prontuario.telegram.application.port.out.ChatResponder;
import io.github.marcosaalbanojunior.prontuario.telegram.application.port.out.TelegramGateway;
import io.github.marcosaalbanojunior.prontuario.telegram.application.port.out.UpdateOffsetStore;
import io.github.marcosaalbanojunior.prontuario.telegram.domain.IncomingMessage;
import io.github.marcosaalbanojunior.prontuario.telegram.domain.Reply;
import io.github.marcosaalbanojunior.prontuario.telegram.domain.TelegramUpdate;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Each update is handled and marked as handled in one transaction; replies are sent only after
 * the commit. A crash in between re-handles the update, and dedupe keeps that from recording twice.
 */
public final class PollUpdatesService implements PollUpdates {

    public static final Duration LONG_POLL = Duration.ofSeconds(50);

    private static final Logger log = LoggerFactory.getLogger(PollUpdatesService.class);

    private final TelegramGateway telegram;
    private final UpdateOffsetStore offsets;
    private final ChatResponder responder;
    private final TransactionRunner transactions;
    private final long allowedChatId;

    public PollUpdatesService(TelegramGateway telegram, UpdateOffsetStore offsets, ChatResponder responder,
                              TransactionRunner transactions, long allowedChatId) {
        this.telegram = telegram;
        this.offsets = offsets;
        this.responder = responder;
        this.transactions = transactions;
        this.allowedChatId = allowedChatId;
    }

    @Override
    public void pollOnce() {
        telegram.fetchUpdates(offsets.nextOffset(), LONG_POLL).forEach(this::handle);
    }

    private void handle(TelegramUpdate update) {
        var allowed = update.message().filter(this::isAllowed);
        var replies = repliesFor(update.updateId(), allowed);
        allowed.ifPresent(message -> send(message, replies));
    }

    private List<Reply> repliesFor(long updateId, Optional<IncomingMessage> message) {
        try {
            return transactions.inTransaction(() -> {
                var replies = message.map(responder::respond).orElse(List.of());
                offsets.markHandled(updateId);
                return replies;
            });
        } catch (RuntimeException e) {
            // Skip the update instead of retrying it forever; the user is told to try again.
            log.error("Failed to handle update {}", updateId, e);
            transactions.inTransaction(() -> offsets.markHandled(updateId));
            return message.isPresent() ? List.of(responder.failureReply()) : List.of();
        }
    }

    private boolean isAllowed(IncomingMessage message) {
        if (message.chatId() == allowedChatId) {
            return true;
        }
        log.warn("Ignored message from chat {} (not TELEGRAM_ALLOWED_CHAT_ID)", message.chatId());
        return false;
    }

    private void send(IncomingMessage message, List<Reply> replies) {
        for (var reply : replies) {
            try {
                telegram.send(message.chatId(), reply);
            } catch (RuntimeException e) {
                log.warn("Failed to send reply to message {}", message.messageId(), e);
            }
        }
    }
}
