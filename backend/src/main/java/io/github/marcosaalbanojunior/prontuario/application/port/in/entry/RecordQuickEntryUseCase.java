package io.github.marcosaalbanojunior.prontuario.application.port.in.entry;

/** Records a quick command (water, sleep, exercise, weight, pain) as a confirmed event. */
public interface RecordQuickEntryUseCase {

    QuickEntryResult record(QuickEntryRequest request);
}
