package io.github.marcosaalbanojunior.prontuario.application.usecase.telegram;

import io.github.marcosaalbanojunior.prontuario.application.port.in.GetGoalProgressUseCase;
import io.github.marcosaalbanojunior.prontuario.application.port.in.HandleChatMessageUseCase;
import io.github.marcosaalbanojunior.prontuario.application.port.in.entry.RecordQuickEntryUseCase;
import io.github.marcosaalbanojunior.prontuario.application.port.in.entry.UndoLastEntryUseCase;
import io.github.marcosaalbanojunior.prontuario.application.port.in.entry.UndoResult;
import io.github.marcosaalbanojunior.prontuario.application.port.out.MessagesPort;
import io.github.marcosaalbanojunior.prontuario.domain.model.telegram.ChatInput;
import io.github.marcosaalbanojunior.prontuario.domain.model.telegram.IncomingMessage;
import io.github.marcosaalbanojunior.prontuario.domain.model.telegram.Reply;
import java.time.Clock;
import java.time.ZoneId;
import java.util.List;

/** Routes a chat message to the right conversation; plain text answers an open question if there is one. */
public final class HandleChatMessageService implements HandleChatMessageUseCase {

    private final QuickEntryConversation quickEntries;
    private final PendingQuestions pending = new PendingQuestions();
    private final UndoLastEntryUseCase undoLastEntry;
    private final GetGoalProgressUseCase goalProgress;
    private final EntryLines entries;
    private final GoalLines goals;
    private final MessagesPort messages;

    public HandleChatMessageService(RecordQuickEntryUseCase recordQuickEntry, UndoLastEntryUseCase undoLastEntry,
                                    GetGoalProgressUseCase goalProgress, MessagesPort messages, Clock clock, ZoneId zone) {
        var format = new PtBrFormat(clock, zone);
        this.messages = messages;
        this.entries = new EntryLines(messages, format);
        this.goals = new GoalLines(messages, format);
        this.quickEntries = new QuickEntryConversation(recordQuickEntry, pending, entries, messages);
        this.undoLastEntry = undoLastEntry;
        this.goalProgress = goalProgress;
    }

    @Override
    public List<Reply> handle(IncomingMessage message) {
        if (message.text().isEmpty()) {
            return reply("telegram.non-text");
        }
        return switch (ChatInput.parse(message.text().get())) {
            case ChatInput.Command command -> command(command, message);
            case ChatInput.PlainText text -> pending.take(message.sentAt())
                    .map(question -> quickEntries.answer(question, text.text(), message))
                    .orElseGet(() -> reply("telegram.free-text"));
        };
    }

    private List<Reply> command(ChatInput.Command command, IncomingMessage message) {
        pending.clear();
        if (quickEntries.handles(command.name())) {
            return quickEntries.command(command.name(), command.args(), message);
        }
        return switch (command.name()) {
            case "desfazer" -> undo();
            case "hoje" -> List.of(Reply.of(goals.today(goalProgress.currentProgress())));
            case "start", "ajuda", "help" -> reply("telegram.help");
            default -> List.of(Reply.of(messages.get("telegram.unknown-command") + "\n\n" + messages.get("telegram.help")));
        };
    }

    private List<Reply> undo() {
        return switch (undoLastEntry.undoLast()) {
            case UndoResult.Undone undone -> List.of(Reply.of(messages.get("telegram.undone", entries.describe(undone.event()))));
            case UndoResult.NothingToUndo ignored -> reply("telegram.nothing-to-undo");
        };
    }

    private List<Reply> reply(String key) {
        return List.of(Reply.of(messages.get(key)));
    }
}
