package io.github.marcosaalbanojunior.prontuario.telegram.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.marcosaalbanojunior.prontuario.common.tx.DirectTransactionRunner;
import io.github.marcosaalbanojunior.prontuario.telegram.application.port.out.ChatResponder;
import io.github.marcosaalbanojunior.prontuario.telegram.application.port.out.UpdateOffsetStore;
import io.github.marcosaalbanojunior.prontuario.telegram.domain.IncomingMessage;
import io.github.marcosaalbanojunior.prontuario.telegram.domain.Reply;
import io.github.marcosaalbanojunior.prontuario.telegram.domain.TelegramUpdate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PollUpdatesServiceTest {

    private static final long OWNER = 42L;
    private static final long STRANGER = 666L;

    private final FakeTelegramGateway telegram = new FakeTelegramGateway();
    private final InMemoryOffsets offsets = new InMemoryOffsets();
    private final RecordingResponder responder = new RecordingResponder();
    private final PollUpdatesService service =
            new PollUpdatesService(telegram, offsets, responder, new DirectTransactionRunner(), OWNER);

    @Test
    void shouldAnswerTheAllowedChat() {
        telegram.receive(update(1, OWNER, "/agua 500"));

        service.pollOnce();

        assertThat(responder.handled).extracting(message -> message.text().orElseThrow()).containsExactly("/agua 500");
        assertThat(telegram.sent()).containsExactly(new FakeTelegramGateway.Sent(OWNER, Reply.of("ok: /agua 500")));
    }

    @Test
    void shouldIgnoreOtherChatsWithoutAnswering() {
        telegram.receive(update(1, STRANGER, "/start"));

        service.pollOnce();

        assertThat(responder.handled).isEmpty();
        assertThat(telegram.sent()).isEmpty();
        assertThat(offsets.nextOffset()).isEqualTo(2);
    }

    @Test
    void shouldAdvanceTheOffsetForEveryUpdateIncludingOnesWithoutMessage() {
        telegram.receive(update(7, OWNER, "/hoje"), new TelegramUpdate(8, Optional.empty()));

        service.pollOnce();
        service.pollOnce();

        assertThat(telegram.requestedOffsets()).containsExactly(0L, 9L);
    }

    @Test
    void shouldSkipAnUpdateThatFailsAndTellTheUser() {
        responder.failOn("/quebra");
        telegram.receive(update(1, OWNER, "/quebra"));

        service.pollOnce();

        assertThat(offsets.nextOffset()).isEqualTo(2);
        assertThat(telegram.sent()).containsExactly(new FakeTelegramGateway.Sent(OWNER, Reply.of("falhou")));
    }

    @Test
    void shouldKeepGoingWhenAReplyCannotBeSent() {
        telegram.failSending();
        telegram.receive(update(1, OWNER, "/agua 500"), update(2, OWNER, "/agua 300"));

        service.pollOnce();

        assertThat(responder.handled).hasSize(2);
        assertThat(offsets.nextOffset()).isEqualTo(3);
    }

    private static TelegramUpdate update(long updateId, long chatId, String text) {
        var message = new IncomingMessage(chatId, updateId * 10, Optional.of(text), Instant.parse("2026-10-12T13:00:00Z"));
        return new TelegramUpdate(updateId, Optional.of(message));
    }

    private static final class InMemoryOffsets implements UpdateOffsetStore {

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

    private static final class RecordingResponder implements ChatResponder {

        private final List<IncomingMessage> handled = new ArrayList<>();
        private String failingText = "";

        void failOn(String text) {
            failingText = text;
        }

        @Override
        public List<Reply> respond(IncomingMessage message) {
            if (message.text().orElse("").equals(failingText)) {
                throw new IllegalStateException("boom");
            }
            handled.add(message);
            return List.of(Reply.of("ok: " + message.text().orElse("")));
        }

        @Override
        public Reply failureReply() {
            return Reply.of("falhou");
        }
    }
}
