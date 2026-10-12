package io.github.marcosaalbanojunior.prontuario.application.usecase.entry;

import io.github.marcosaalbanojunior.prontuario.application.port.in.entry.SourceChannel;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.CreatedVia;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.SourceKind;

/** How each inbound channel is recorded on the source and the event. */
final class Channels {

    private Channels() {}

    static SourceKind sourceKind(SourceChannel channel) {
        return switch (channel) {
            case TELEGRAM_COMMAND -> SourceKind.TELEGRAM_COMMAND;
            case REMINDER_REPLY -> SourceKind.REMINDER_REPLY;
            case WEB -> SourceKind.WEB;
        };
    }

    static CreatedVia createdVia(SourceChannel channel) {
        return switch (channel) {
            case TELEGRAM_COMMAND -> CreatedVia.COMMAND;
            case REMINDER_REPLY -> CreatedVia.REMINDER_REPLY;
            case WEB -> CreatedVia.WEB;
        };
    }
}
