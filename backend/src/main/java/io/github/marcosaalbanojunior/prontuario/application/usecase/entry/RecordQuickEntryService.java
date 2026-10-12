package io.github.marcosaalbanojunior.prontuario.application.usecase.entry;

import io.github.marcosaalbanojunior.prontuario.application.port.in.entry.QuickEntryRequest;
import io.github.marcosaalbanojunior.prontuario.application.port.in.entry.QuickEntryResult;
import io.github.marcosaalbanojunior.prontuario.application.port.in.entry.RecordQuickEntryUseCase;
import io.github.marcosaalbanojunior.prontuario.application.port.in.entry.SourceRef;
import io.github.marcosaalbanojunior.prontuario.application.port.out.EventRepositoryPort;
import io.github.marcosaalbanojunior.prontuario.application.port.out.IdGeneratorPort;
import io.github.marcosaalbanojunior.prontuario.application.port.out.SourceRepositoryPort;
import io.github.marcosaalbanojunior.prontuario.application.port.out.TransactionPort;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.Event;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.Source;
import io.github.marcosaalbanojunior.prontuario.domain.model.quickentry.ParseOutcome;
import io.github.marcosaalbanojunior.prontuario.domain.model.quickentry.QuickEntry;
import io.github.marcosaalbanojunior.prontuario.domain.service.quickentry.QuickEntryEvents;
import io.github.marcosaalbanojunior.prontuario.domain.service.quickentry.QuickEntryParser;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public final class RecordQuickEntryService implements RecordQuickEntryUseCase {

    private final QuickEntryParser parser;
    private final EventRepositoryPort events;
    private final SourceRepositoryPort sources;
    private final IdGeneratorPort ids;
    private final TransactionPort transactions;
    private final Clock clock;
    private final ZoneId zone;

    @Override
    public QuickEntryResult record(QuickEntryRequest request) {
        return switch (parser.parse(request.command(), request.args())) {
            case ParseOutcome.NeedsMore needsMore -> new QuickEntryResult.NeedsMore(needsMore.missing());
            case ParseOutcome.Rejected rejected -> new QuickEntryResult.Rejected(rejected.reason());
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
        log.info("Event recorded. eventId={} type={}", event.id(), details.type());
        return new QuickEntryResult.Recorded(event);
    }
}
