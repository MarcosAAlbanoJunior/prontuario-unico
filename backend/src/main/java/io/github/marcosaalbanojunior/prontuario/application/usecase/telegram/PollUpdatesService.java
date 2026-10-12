package io.github.marcosaalbanojunior.prontuario.application.usecase.telegram;

import io.github.marcosaalbanojunior.prontuario.application.port.in.HandleChatMessageUseCase;
import io.github.marcosaalbanojunior.prontuario.application.port.in.PollUpdatesUseCase;
import io.github.marcosaalbanojunior.prontuario.application.port.out.MessagesPort;
import io.github.marcosaalbanojunior.prontuario.application.port.out.TransactionPort;
import io.github.marcosaalbanojunior.prontuario.application.port.out.telegram.TelegramGatewayPort;
import io.github.marcosaalbanojunior.prontuario.application.port.out.telegram.UpdateOffsetRepositoryPort;
import io.github.marcosaalbanojunior.prontuario.domain.model.telegram.ChatInput;
import io.github.marcosaalbanojunior.prontuario.domain.model.telegram.IncomingMessage;
import io.github.marcosaalbanojunior.prontuario.domain.model.telegram.Reply;
import io.github.marcosaalbanojunior.prontuario.domain.model.telegram.TelegramUpdate;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

/**
 * Each update is handled and marked as handled in one transaction; replies are sent only after
 * the commit. A crash in between re-handles the update, and dedupe keeps that from recording twice.
 */
@Slf4j
@RequiredArgsConstructor
public final class PollUpdatesService implements PollUpdatesUseCase {

    public static final Duration LONG_POLL = Duration.ofSeconds(50);

    private static final String TRACE_ID = "traceId";

    private final TelegramGatewayPort telegram;
    private final UpdateOffsetRepositoryPort offsets;
    private final HandleChatMessageUseCase chat;
    private final MessagesPort messages;
    private final TransactionPort transactions;
    private final long allowedChatId;

    @Override
    public void pollOnce() {
        telegram.fetchUpdates(offsets.nextOffset(), LONG_POLL).forEach(this::handleTraced);
    }

    private void handleTraced(TelegramUpdate update) {
        try (var ignored = MDC.putCloseable(TRACE_ID, UUID.randomUUID().toString())) {
            handle(update);
        }
    }

    private void handle(TelegramUpdate update) {
        var message = update.message();
        if (message.isEmpty()) {
            markHandled(update.updateId());
            return;
        }
        if (message.get().chatId() != allowedChatId) {
            ignore(update.updateId(), message.get());
            return;
        }
        send(message.get(), repliesFor(update.updateId(), message.get()));
    }

    private List<Reply> repliesFor(long updateId, IncomingMessage message) {
        try {
            return transactions.inTransaction(() -> {
                var replies = chat.handle(message);
                offsets.markHandled(updateId);
                return replies;
            });
        } catch (RuntimeException e) {
            // Skip the update instead of retrying it forever; the user is told to try again.
            log.error("Failed to handle update. updateId={}", updateId, e);
            markHandled(updateId);
            return List.of(Reply.of(messages.get("telegram.failure")));
        }
    }

    private void markHandled(long updateId) {
        transactions.inTransaction(() -> offsets.markHandled(updateId));
    }

    private void ignore(long updateId, IncomingMessage message) {
        markHandled(updateId);
        log.info("Update from a chat that is not allowed was ignored. updateId={}", updateId);
        if (isStart(message)) {
            // The only answer to an unknown chat: its own id, so the owner can configure TELEGRAM_ALLOWED_CHAT_ID.
            send(message, List.of(Reply.of(messages.get("telegram.your-chat-id", String.valueOf(message.chatId())))));
        }
    }

    private static boolean isStart(IncomingMessage message) {
        return message.text().map(ChatInput::parse)
                .filter(input -> input instanceof ChatInput.Command command && command.name().equals("start"))
                .isPresent();
    }

    private void send(IncomingMessage message, List<Reply> replies) {
        for (var reply : replies) {
            try {
                telegram.send(message.chatId(), reply);
            } catch (RuntimeException e) {
                log.warn("Failed to send a reply. messageId={} reason={}", message.messageId(), e.getClass().getSimpleName());
            }
        }
    }
}
