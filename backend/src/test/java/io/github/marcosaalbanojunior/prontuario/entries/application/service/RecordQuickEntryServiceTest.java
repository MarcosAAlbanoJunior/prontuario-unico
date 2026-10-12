package io.github.marcosaalbanojunior.prontuario.entries.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.marcosaalbanojunior.prontuario.common.id.SequentialIdGenerator;
import io.github.marcosaalbanojunior.prontuario.common.tx.DirectTransactionRunner;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.MissingField;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.QuickEntryRequest;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.QuickEntryResult;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.QuickEntryType;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.RejectionCause;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.SourceChannel;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.SourceRef;
import io.github.marcosaalbanojunior.prontuario.entries.domain.BodyRegion;
import io.github.marcosaalbanojunior.prontuario.entries.domain.CreatedVia;
import io.github.marcosaalbanojunior.prontuario.entries.domain.EventStatus;
import io.github.marcosaalbanojunior.prontuario.entries.domain.SourceKind;
import io.github.marcosaalbanojunior.prontuario.entries.domain.quick.BodyRegionVocabulary;
import io.github.marcosaalbanojunior.prontuario.entries.domain.quick.QuickEntryParser;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RecordQuickEntryServiceTest {

    private static final Instant SENT_AT = Instant.parse("2026-10-12T13:00:00Z");
    private static final Instant PROCESSED_AT = SENT_AT.plusSeconds(90);

    private final InMemoryEventRepository events = new InMemoryEventRepository();
    private final InMemorySourceRepository sources = new InMemorySourceRepository();
    private final RecordQuickEntryService service = new RecordQuickEntryService(
            new QuickEntryParser(new BodyRegionVocabulary(Map.of(BodyRegion.RIGHT_KNEE, List.of("joelho direito")))),
            events, sources, new SequentialIdGenerator(), new DirectTransactionRunner(),
            Clock.fixed(PROCESSED_AT, ZoneOffset.UTC), ZoneId.of("America/Sao_Paulo"));

    @Test
    void shouldRecordConfirmedEventWithItsSource() {
        var result = service.record(request(QuickEntryType.WATER, "500", 42L));

        assertThat(result).isInstanceOf(QuickEntryResult.Recorded.class);
        var summary = ((QuickEntryResult.Recorded) result).entry();
        assertThat(summary.type()).isEqualTo("water");
        assertThat(summary.value()).contains(new BigDecimal("500"));
        assertThat(summary.unit()).contains("ml");

        var event = events.all().getFirst();
        var source = sources.all().getFirst();
        assertThat(event.status()).isEqualTo(EventStatus.CONFIRMED);
        assertThat(event.createdVia()).isEqualTo(CreatedVia.COMMAND);
        assertThat(event.sourceId()).isEqualTo(source.id());
        assertThat(source.kind()).isEqualTo(SourceKind.TELEGRAM_COMMAND);
        assertThat(source.rawText()).contains("/agua 500");
        assertThat(source.telegramMessageId()).contains(42L);
    }

    @Test
    void shouldPlaceEntryWhenTheMessageWasSentNotWhenItWasProcessed() {
        service.record(request(QuickEntryType.WATER, "500", 42L));

        var event = events.all().getFirst();
        assertThat(event.details().occurredStart()).isEqualTo(SENT_AT);
        assertThat(event.recordedAt()).isEqualTo(PROCESSED_AT);
    }

    @Test
    void shouldNotRecordTheSameMessageTwice() {
        service.record(request(QuickEntryType.WATER, "500", 42L));

        var second = service.record(request(QuickEntryType.WATER, "500", 42L));

        assertThat(second).isInstanceOf(QuickEntryResult.AlreadyRecorded.class);
        assertThat(events.all()).hasSize(1);
    }

    @Test
    void shouldAskForMissingDataWithoutRecording() {
        var result = service.record(request(QuickEntryType.PAIN, "4", 43L));

        assertThat(result).isEqualTo(new QuickEntryResult.NeedsMore(MissingField.BODY_REGION));
        assertThat(events.all()).isEmpty();
        assertThat(sources.all()).isEmpty();
    }

    @Test
    void shouldRejectInvalidDataWithoutRecording() {
        var result = service.record(request(QuickEntryType.WEIGHT, "900", 44L));

        assertThat(result).isEqualTo(new QuickEntryResult.Rejected(RejectionCause.OUT_OF_RANGE));
        assertThat(events.all()).isEmpty();
    }

    private static QuickEntryRequest request(QuickEntryType type, String args, long messageId) {
        var command = "/" + switch (type) {
            case WATER -> "agua";
            case SLEEP -> "sono";
            case EXERCISE -> "treino";
            case WEIGHT -> "peso";
            case PAIN -> "dor";
        };
        var source = new SourceRef(SourceChannel.TELEGRAM_COMMAND, command + " " + args, Optional.of(messageId), SENT_AT);
        return new QuickEntryRequest(type, args, source);
    }
}
