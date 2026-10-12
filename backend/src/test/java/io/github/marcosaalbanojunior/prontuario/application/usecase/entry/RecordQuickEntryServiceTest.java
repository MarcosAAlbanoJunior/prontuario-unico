package io.github.marcosaalbanojunior.prontuario.application.usecase.entry;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.marcosaalbanojunior.prontuario.application.port.in.entry.QuickEntryRequest;
import io.github.marcosaalbanojunior.prontuario.application.port.in.entry.QuickEntryResult;
import io.github.marcosaalbanojunior.prontuario.application.port.in.entry.SourceChannel;
import io.github.marcosaalbanojunior.prontuario.application.port.in.entry.SourceRef;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.BodyRegion;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.CreatedVia;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.EventStatus;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.SourceKind;
import io.github.marcosaalbanojunior.prontuario.domain.model.quickentry.MissingInfo;
import io.github.marcosaalbanojunior.prontuario.domain.model.quickentry.QuickCommand;
import io.github.marcosaalbanojunior.prontuario.domain.model.quickentry.RejectionReason;
import io.github.marcosaalbanojunior.prontuario.domain.service.quickentry.BodyRegionVocabulary;
import io.github.marcosaalbanojunior.prontuario.domain.service.quickentry.QuickEntryParser;
import io.github.marcosaalbanojunior.prontuario.support.DirectTransactions;
import io.github.marcosaalbanojunior.prontuario.support.InMemoryEventRepository;
import io.github.marcosaalbanojunior.prontuario.support.InMemorySourceRepository;
import io.github.marcosaalbanojunior.prontuario.support.SequentialIdGenerator;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("RecordQuickEntryService")
class RecordQuickEntryServiceTest {

    private static final Instant SENT_AT = Instant.parse("2026-10-12T13:00:00Z");
    private static final Instant PROCESSED_AT = SENT_AT.plusSeconds(90);

    private final InMemoryEventRepository events = new InMemoryEventRepository();
    private final InMemorySourceRepository sources = new InMemorySourceRepository();
    private final RecordQuickEntryService service = new RecordQuickEntryService(
            new QuickEntryParser(new BodyRegionVocabulary(Map.of(BodyRegion.RIGHT_KNEE, List.of("joelho direito")))),
            events, sources, new SequentialIdGenerator(), new DirectTransactions(),
            Clock.fixed(PROCESSED_AT, ZoneOffset.UTC), ZoneId.of("America/Sao_Paulo"));

    @Nested
    @DisplayName("record(...) with valid data")
    class Valid {

        @Test
        @DisplayName("stores a confirmed event linked to its source")
        void should_storeConfirmedEventAndSource_when_dataIsValid() {
            var result = service.record(request(QuickCommand.WATER, "500", SourceChannel.TELEGRAM_COMMAND, Optional.of(42L)));

            var event = events.all().getFirst();
            var source = sources.all().getFirst();
            assertThat(result).isEqualTo(new QuickEntryResult.Recorded(event));
            assertThat(event.status()).isEqualTo(EventStatus.CONFIRMED);
            assertThat(event.details().measurement().orElseThrow().value()).isEqualTo(new BigDecimal("500"));
            assertThat(event.sourceId()).isEqualTo(source.id());
            assertThat(source.rawText()).contains("/agua 500");
            assertThat(source.telegramMessageId()).contains(42L);
        }

        @Test
        @DisplayName("places the entry when the message was sent, not when it was processed")
        void should_useSendTime_when_processedLater() {
            service.record(request(QuickCommand.WATER, "500", SourceChannel.TELEGRAM_COMMAND, Optional.of(42L)));

            var event = events.all().getFirst();
            assertThat(event.details().occurredStart()).isEqualTo(SENT_AT);
            assertThat(event.recordedAt()).isEqualTo(PROCESSED_AT);
        }

        @ParameterizedTest(name = "{0} is stored as source {1} and created via {2}")
        @CsvSource({
            "TELEGRAM_COMMAND, TELEGRAM_COMMAND, COMMAND",
            "REMINDER_REPLY,   REMINDER_REPLY,   REMINDER_REPLY",
            "WEB,              WEB,              WEB",
        })
        @DisplayName("records each inbound channel on the source and the event")
        void should_recordChannel_when_entryComesFromIt(SourceChannel channel, SourceKind kind, CreatedVia createdVia) {
            service.record(request(QuickCommand.WATER, "500", channel, Optional.empty()));

            assertThat(sources.all().getFirst().kind()).isEqualTo(kind);
            assertThat(events.all().getFirst().createdVia()).isEqualTo(createdVia);
        }
    }

    @Nested
    @DisplayName("record(...) with a message already recorded")
    class AlreadyRecorded {

        @Test
        @DisplayName("records nothing the second time")
        void should_returnAlreadyRecorded_when_sameMessageComesAgain() {
            service.record(request(QuickCommand.WATER, "500", SourceChannel.TELEGRAM_COMMAND, Optional.of(42L)));

            var second = service.record(request(QuickCommand.WATER, "500", SourceChannel.TELEGRAM_COMMAND, Optional.of(42L)));

            assertThat(second).isInstanceOf(QuickEntryResult.AlreadyRecorded.class);
            assertThat(events.all()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("record(...) with missing or invalid data")
    class Incomplete {

        @Test
        @DisplayName("asks for what is missing and records nothing")
        void should_returnNeedsMore_when_essentialDataIsMissing() {
            var result = service.record(request(QuickCommand.PAIN, "4", SourceChannel.TELEGRAM_COMMAND, Optional.of(43L)));

            assertThat(result).isEqualTo(new QuickEntryResult.NeedsMore(MissingInfo.BODY_REGION));
            assertThat(events.all()).isEmpty();
            assertThat(sources.all()).isEmpty();
        }

        @Test
        @DisplayName("rejects invalid data and records nothing")
        void should_returnRejected_when_dataIsInvalid() {
            var result = service.record(request(QuickCommand.WEIGHT, "900", SourceChannel.TELEGRAM_COMMAND, Optional.of(44L)));

            assertThat(result).isEqualTo(new QuickEntryResult.Rejected(RejectionReason.OUT_OF_RANGE));
            assertThat(events.all()).isEmpty();
        }
    }

    private static QuickEntryRequest request(QuickCommand command, String args, SourceChannel channel, Optional<Long> messageId) {
        var name = command == QuickCommand.WATER ? "/agua" : command == QuickCommand.PAIN ? "/dor" : "/peso";
        return new QuickEntryRequest(command, args, new SourceRef(channel, name + " " + args, messageId, SENT_AT));
    }
}
