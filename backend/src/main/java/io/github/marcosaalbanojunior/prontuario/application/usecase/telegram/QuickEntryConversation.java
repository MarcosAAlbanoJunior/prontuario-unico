package io.github.marcosaalbanojunior.prontuario.application.usecase.telegram;

import io.github.marcosaalbanojunior.prontuario.application.port.in.entry.QuickEntryRequest;
import io.github.marcosaalbanojunior.prontuario.application.port.in.entry.QuickEntryResult;
import io.github.marcosaalbanojunior.prontuario.application.port.in.entry.RecordQuickEntryUseCase;
import io.github.marcosaalbanojunior.prontuario.application.port.in.entry.SourceChannel;
import io.github.marcosaalbanojunior.prontuario.application.port.in.entry.SourceRef;
import io.github.marcosaalbanojunior.prontuario.application.port.out.MessagesPort;
import io.github.marcosaalbanojunior.prontuario.domain.model.quickentry.QuickCommand;
import io.github.marcosaalbanojunior.prontuario.domain.model.telegram.IncomingMessage;
import io.github.marcosaalbanojunior.prontuario.domain.model.telegram.PendingQuestion;
import io.github.marcosaalbanojunior.prontuario.domain.model.telegram.Reply;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/** /agua, /sono, /treino, /peso and /dor, including the follow-up question when data is missing. */
@RequiredArgsConstructor
final class QuickEntryConversation {

    private static final Map<String, QuickCommand> COMMANDS = Map.of(
            "agua", QuickCommand.WATER,
            "sono", QuickCommand.SLEEP,
            "treino", QuickCommand.EXERCISE,
            "peso", QuickCommand.WEIGHT,
            "dor", QuickCommand.PAIN);

    private final RecordQuickEntryUseCase recordQuickEntry;
    private final PendingQuestions pending;
    private final EntryLines entries;
    private final MessagesPort messages;

    boolean handles(String command) {
        return COMMANDS.containsKey(command);
    }

    List<Reply> command(String command, String args, IncomingMessage message) {
        var rawText = ("/" + command + " " + args).strip();
        var source = new SourceRef(SourceChannel.TELEGRAM_COMMAND, rawText, Optional.of(message.messageId()), message.sentAt());
        return switch (recordQuickEntry.record(new QuickEntryRequest(COMMANDS.get(command), args, source))) {
            case QuickEntryResult.Recorded recorded -> List.of(Reply.of(messages.get("telegram.recorded", entries.describe(recorded.event()))));
            case QuickEntryResult.NeedsMore needsMore -> ask(command, args, message, MessageKeys.of(needsMore.missing()));
            case QuickEntryResult.Rejected rejected -> List.of(Reply.of(rejection(command, MessageKeys.of(rejected.reason()))));
            case QuickEntryResult.AlreadyRecorded ignored -> List.of();
        };
    }

    List<Reply> answer(PendingQuestion question, String answer, IncomingMessage message) {
        return command(question.command(), question.argsWithAnswer(answer), message);
    }

    private List<Reply> ask(String command, String args, IncomingMessage message, String missing) {
        pending.ask(new PendingQuestion(command, args, message.sentAt()));
        return List.of(Reply.question(messages.get("telegram.ask." + missing)));
    }

    private String rejection(String command, String reason) {
        return messages.get("telegram.reject." + reason, messages.get("telegram.usage." + command));
    }
}
