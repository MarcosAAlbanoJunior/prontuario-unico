package io.github.marcosaalbanojunior.prontuario.entries.application.port.in;

/** Records a quick command (water, sleep, exercise, weight, pain) as a confirmed event. */
public interface RecordQuickEntry {

    QuickEntryResult record(QuickEntryRequest request);
}
