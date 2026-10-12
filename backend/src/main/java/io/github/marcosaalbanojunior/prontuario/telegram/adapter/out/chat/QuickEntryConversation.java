package io.github.marcosaalbanojunior.prontuario.telegram.adapter.out.chat;

import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.QuickEntryRequest;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.QuickEntryResult;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.QuickEntryType;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.RecordQuickEntry;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.SourceChannel;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.SourceRef;
import io.github.marcosaalbanojunior.prontuario.telegram.domain.IncomingMessage;
import io.github.marcosaalbanojunior.prontuario.telegram.domain.PendingQuestion;
import io.github.marcosaalbanojunior.prontuario.telegram.domain.Reply;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** /agua, /sono, /treino, /peso and /dor, including the follow-up question when data is missing. */
final class QuickEntryConversation {

    private static final Map<String, QuickEntryType> COMMANDS = Map.of(
            "agua", QuickEntryType.WATER,
            "sono", QuickEntryType.SLEEP,
            "treino", QuickEntryType.EXERCISE,
            "peso", QuickEntryType.WEIGHT,
            "dor", QuickEntryType.PAIN);

    private final RecordQuickEntry recordQuickEntry;
    private final PendingQuestions pending;
    private final EntryLines entries;
    private final Texts texts;

    QuickEntryConversation(RecordQuickEntry recordQuickEntry, PendingQuestions pending, EntryLines entries, Texts texts) {
        this.recordQuickEntry = recordQuickEntry;
        this.pending = pending;
        this.entries = entries;
        this.texts = texts;
    }

    boolean handles(String command) {
        return COMMANDS.containsKey(command);
    }

    List<Reply> command(String command, String args, IncomingMessage message) {
        var rawText = ("/" + command + " " + args).strip();
        var source = new SourceRef(SourceChannel.TELEGRAM_COMMAND, rawText, Optional.of(message.messageId()), message.sentAt());
        var result = recordQuickEntry.record(new QuickEntryRequest(COMMANDS.get(command), args, source));
        return switch (result) {
            case QuickEntryResult.Recorded recorded -> List.of(Reply.of(texts.get("telegram.recorded", entries.describe(recorded.entry()))));
            case QuickEntryResult.NeedsMore needsMore -> ask(command, args, message, key(needsMore.missing()));
            case QuickEntryResult.Rejected rejected -> List.of(Reply.of(rejection(command, key(rejected.cause()))));
            case QuickEntryResult.AlreadyRecorded ignored -> List.of();
        };
    }

    List<Reply> answer(PendingQuestion question, String answer, IncomingMessage message) {
        return command(question.command(), question.argsWithAnswer(answer), message);
    }

    private List<Reply> ask(String command, String args, IncomingMessage message, String missing) {
        pending.ask(new PendingQuestion(command, args, message.sentAt()));
        return List.of(Reply.question(texts.get("telegram.ask." + missing)));
    }

    private String rejection(String command, String cause) {
        return texts.get("telegram.reject." + cause, texts.get("telegram.usage." + command));
    }

    private static String key(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }
}
