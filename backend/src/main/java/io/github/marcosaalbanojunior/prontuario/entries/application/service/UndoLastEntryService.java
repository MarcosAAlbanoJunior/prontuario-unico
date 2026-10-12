package io.github.marcosaalbanojunior.prontuario.entries.application.service;

import io.github.marcosaalbanojunior.prontuario.common.tx.TransactionRunner;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.UndoLastEntry;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.UndoResult;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.out.EventRepository;

public final class UndoLastEntryService implements UndoLastEntry {

    private final EventRepository events;
    private final TransactionRunner transactions;

    public UndoLastEntryService(EventRepository events, TransactionRunner transactions) {
        this.events = events;
        this.transactions = transactions;
    }

    @Override
    public UndoResult undoLast() {
        return transactions.inTransaction(() -> events.latestNotDiscarded()
                .<UndoResult>map(event -> {
                    var discarded = event.discard();
                    events.updateStatus(discarded.id(), discarded.status());
                    return new UndoResult.Undone(EntrySummaries.of(discarded));
                })
                .orElseGet(UndoResult.NothingToUndo::new));
    }
}
