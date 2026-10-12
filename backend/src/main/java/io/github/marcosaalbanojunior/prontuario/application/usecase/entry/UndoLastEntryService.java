package io.github.marcosaalbanojunior.prontuario.application.usecase.entry;

import io.github.marcosaalbanojunior.prontuario.application.port.in.entry.UndoLastEntryUseCase;
import io.github.marcosaalbanojunior.prontuario.application.port.in.entry.UndoResult;
import io.github.marcosaalbanojunior.prontuario.application.port.out.EventRepositoryPort;
import io.github.marcosaalbanojunior.prontuario.application.port.out.TransactionPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public final class UndoLastEntryService implements UndoLastEntryUseCase {

    private final EventRepositoryPort events;
    private final TransactionPort transactions;

    @Override
    public UndoResult undoLast() {
        return transactions.inTransaction(() -> events.latestNotDiscarded()
                .<UndoResult>map(event -> {
                    var discarded = event.discard();
                    events.updateStatus(discarded.id(), discarded.status());
                    log.info("Event discarded. eventId={}", discarded.id());
                    return new UndoResult.Undone(discarded);
                })
                .orElseGet(UndoResult.NothingToUndo::new));
    }
}
