package io.github.marcosaalbanojunior.prontuario.entries.application.service;

import io.github.marcosaalbanojunior.prontuario.common.id.IdGenerator;
import io.github.marcosaalbanojunior.prontuario.common.tx.TransactionRunner;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.MissingField;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.QuickEntryRequest;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.QuickEntryResult;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.RecordQuickEntry;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.RejectionCause;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.SourceRef;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.out.EventRepository;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.out.SourceRepository;
import io.github.marcosaalbanojunior.prontuario.entries.domain.Event;
import io.github.marcosaalbanojunior.prontuario.entries.domain.Source;
import io.github.marcosaalbanojunior.prontuario.entries.domain.quick.ParseOutcome;
import io.github.marcosaalbanojunior.prontuario.entries.domain.quick.QuickCommand;
import io.github.marcosaalbanojunior.prontuario.entries.domain.quick.QuickEntry;
import io.github.marcosaalbanojunior.prontuario.entries.domain.quick.QuickEntryEvents;
import io.github.marcosaalbanojunior.prontuario.entries.domain.quick.QuickEntryParser;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Optional;

public final class RecordQuickEntryService implements RecordQuickEntry {

    private final QuickEntryParser parser;
    private final EventRepository events;
    private final SourceRepository sources;
    private final IdGenerator ids;
    private final TransactionRunner transactions;
    private final Clock clock;
    private final ZoneId zone;

    public RecordQuickEntryService(QuickEntryParser parser, EventRepository events, SourceRepository sources,
                                   IdGenerator ids, TransactionRunner transactions, Clock clock, ZoneId zone) {
        this.parser = parser;
        this.events = events;
        this.sources = sources;
        this.ids = ids;
        this.transactions = transactions;
        this.clock = clock;
        this.zone = zone;
    }

    @Override
    public QuickEntryResult record(QuickEntryRequest request) {
        var outcome = parser.parse(QuickCommand.valueOf(request.type().name()), request.args());
        return switch (outcome) {
            case ParseOutcome.NeedsMore needsMore -> new QuickEntryResult.NeedsMore(MissingField.valueOf(needsMore.missing().name()));
            case ParseOutcome.Rejected rejected -> new QuickEntryResult.Rejected(RejectionCause.valueOf(rejected.reason().name()));
            case ParseOutcome.Parsed parsed -> transactions.inTransaction(() -> store(parsed.entry(), request.source()));
        };
    }

    private QuickEntryResult store(QuickEntry entry, SourceRef ref) {
        if (ref.telegramMessageId().filter(sources::existsByTelegramMessageId).isPresent()) {
            return new QuickEntryResult.AlreadyRecorded();
        }
        var source = new Source(ids.newId(), Channels.sourceKind(ref.channel()), Optional.of(ref.rawText()),
                ref.telegramMessageId(), ref.receivedAt());
        var details = QuickEntryEvents.toDetails(entry, ref.receivedAt().atZone(zone));
        var event = Event.confirmed(ids.newId(), details, source.id(), Channels.createdVia(ref.channel()), clock.instant());
        sources.add(source);
        events.add(event);
        return new QuickEntryResult.Recorded(EntrySummaries.of(event));
    }
}
