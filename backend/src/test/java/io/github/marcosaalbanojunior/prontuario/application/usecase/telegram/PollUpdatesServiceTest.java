package io.github.marcosaalbanojunior.prontuario.application.usecase.telegram;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.marcosaalbanojunior.prontuario.application.port.out.telegram.UpdateOffsetRepositoryPort;
import io.github.marcosaalbanojunior.prontuario.domain.model.telegram.IncomingMessage;
import io.github.marcosaalbanojunior.prontuario.domain.model.telegram.Reply;
import io.github.marcosaalbanojunior.prontuario.domain.model.telegram.TelegramUpdate;
import io.github.marcosaalbanojunior.prontuario.support.DirectTransactions;
import io.github.marcosaalbanojunior.prontuario.support.FakeTelegramGateway.Sent;
import io.github.marcosaalbanojunior.prontuario.support.FakeTelegramGateway;
import io.github.marcosaalbanojunior.prontuario.support.PtBrMessages;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

@DisplayName("PollUpdatesService.pollOnce()")
class PollUpdatesServiceTest {

    private static final long OWNER = 42L;
    private static final long STRANGER = 666L;

    private final FakeTelegramGateway telegram = new FakeTelegramGateway();
    private final InMemoryOffsets offsets = new InMemoryOffsets();
    private final List<IncomingMessage> handled = new ArrayList<>();
    private final List<String> traceIds = new ArrayList<>();
    private final PollUpdatesService service = new PollUpdatesService(telegram, offsets, this::handle, PtBrMessages.load(),
            new DirectTransactions(), OWNER);

    @Nested
    @DisplayName("the allowed chat")
    class AllowedChat {

        @Test
        @DisplayName("is answered with the replies of the chat use case")
        void should_sendReplies_when_chatIsAllowed() {
            telegram.receive(update(1, OWNER, "/agua 500"));

            service.pollOnce();

            assertThat(handled).extracting(message -> message.text().orElseThrow()).containsExactly("/agua 500");
            assertThat(telegram.sent()).containsExactly(new Sent(OWNER, Reply.of("ok: /agua 500")));
        }

        @Test
        @DisplayName("gets a try-again reply and the update is skipped when handling fails")
        void should_skipUpdateAndApologize_when_handlingFails() {
            telegram.receive(update(1, OWNER, "/quebra"));

            service.pollOnce();

            assertThat(offsets.nextOffset()).isEqualTo(2);
            assertThat(telegram.sent()).containsExactly(
                    new Sent(OWNER, Reply.of("Algo deu errado ao registrar sua mensagem. Tente de novo.")));
        }

        @Test
        @DisplayName("keeps being served when a reply cannot be sent")
        void should_keepGoing_when_sendingFails() {
            telegram.failSending();
            telegram.receive(update(1, OWNER, "/agua 500"), update(2, OWNER, "/agua 300"));

            service.pollOnce();

            assertThat(handled).hasSize(2);
            assertThat(offsets.nextOffset()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("other chats")
    class OtherChats {

        @Test
        @DisplayName("are ignored without an answer")
        void should_ignoreSilently_when_chatIsNotAllowed() {
            telegram.receive(update(1, STRANGER, "/agua 500"), update(2, STRANGER, "oi"), new TelegramUpdate(3,
                    Optional.of(new IncomingMessage(STRANGER, 30, Optional.empty(), Instant.EPOCH))));

            service.pollOnce();

            assertThat(handled).isEmpty();
            assertThat(telegram.sent()).isEmpty();
            assertThat(offsets.nextOffset()).isEqualTo(4);
        }

        @Test
        @DisplayName("get only their own chat id in reply to /start")
        void should_replyChatId_when_strangerSendsStart() {
            telegram.receive(update(1, STRANGER, "/start"));

            service.pollOnce();

            assertThat(handled).isEmpty();
            assertThat(telegram.sent()).singleElement().satisfies(sent -> {
                assertThat(sent.chatId()).isEqualTo(STRANGER);
                assertThat(sent.reply().text()).startsWith("Seu chat ID é 666.");
            });
        }
    }

    @Nested
    @DisplayName("offsets and tracing")
    class OffsetsAndTracing {

        @Test
        @DisplayName("advance for every update, including ones without a message")
        void should_advanceOffset_when_updateHasNoMessage() {
            telegram.receive(update(7, OWNER, "/hoje"), new TelegramUpdate(8, Optional.empty()));

            service.pollOnce();
            service.pollOnce();

            assertThat(telegram.requestedOffsets()).containsExactly(0L, 9L);
        }

        @Test
        @DisplayName("give each update its own trace id and clear it afterwards")
        void should_traceEachUpdate_when_handling() {
            telegram.receive(update(1, OWNER, "/agua 500"), update(2, OWNER, "/agua 300"));

            service.pollOnce();

            assertThat(traceIds).hasSize(2).doesNotContainNull().doesNotHaveDuplicates();
            assertThat(MDC.get("traceId")).isNull();
        }
    }

    private List<Reply> handle(IncomingMessage message) {
        if (message.text().orElse("").equals("/quebra")) {
            throw new IllegalStateException("boom");
        }
        handled.add(message);
        traceIds.add(MDC.get("traceId"));
        return List.of(Reply.of("ok: " + message.text().orElse("")));
    }

    private static TelegramUpdate update(long updateId, long chatId, String text) {
        var message = new IncomingMessage(chatId, updateId * 10, Optional.of(text), Instant.parse("2026-10-12T13:00:00Z"));
        return new TelegramUpdate(updateId, Optional.of(message));
    }

    private static final class InMemoryOffsets implements UpdateOffsetRepositoryPort {

        private long last = -1;

        @Override
        public long nextOffset() {
            return last + 1;
        }

        @Override
        public void markHandled(long updateId) {
            last = Math.max(last, updateId);
        }
    }
}
