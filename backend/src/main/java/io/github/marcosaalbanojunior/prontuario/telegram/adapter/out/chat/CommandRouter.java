package io.github.marcosaalbanojunior.prontuario.telegram.adapter.out.chat;

import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.UndoLastEntry;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.UndoResult;
import io.github.marcosaalbanojunior.prontuario.goals.application.port.in.GetGoalProgress;
import io.github.marcosaalbanojunior.prontuario.telegram.application.port.out.ChatResponder;
import io.github.marcosaalbanojunior.prontuario.telegram.domain.ChatInput;
import io.github.marcosaalbanojunior.prontuario.telegram.domain.IncomingMessage;
import io.github.marcosaalbanojunior.prontuario.telegram.domain.Reply;
import java.util.List;

/** Routes a chat message to the right conversation; plain text answers an open question if there is one. */
public final class CommandRouter implements ChatResponder {

    private final QuickEntryConversation quickEntries;
    private final PendingQuestions pending;
    private final UndoLastEntry undoLastEntry;
    private final GetGoalProgress goalProgress;
    private final EntryLines entries;
    private final GoalLines goals;
    private final Texts texts;

    public CommandRouter(ChatPorts ports, Texts texts, PtBrFormat format) {
        this.texts = texts;
        this.pending = new PendingQuestions();
        this.entries = new EntryLines(texts, format);
        this.goals = new GoalLines(texts, format);
        this.quickEntries = new QuickEntryConversation(ports.recordQuickEntry(), pending, entries, texts);
        this.undoLastEntry = ports.undoLastEntry();
        this.goalProgress = ports.goalProgress();
    }

    @Override
    public List<Reply> respond(IncomingMessage message) {
        if (message.text().isEmpty()) {
            return reply("telegram.non_text");
        }
        return switch (ChatInput.parse(message.text().get())) {
            case ChatInput.Command command -> command(command, message);
            case ChatInput.PlainText text -> pending.take(message.sentAt())
                    .map(question -> quickEntries.answer(question, text.text(), message))
                    .orElseGet(() -> reply("telegram.free_text"));
        };
    }

    @Override
    public Reply failureReply() {
        return Reply.of(texts.get("telegram.failure"));
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
            default -> List.of(Reply.of(texts.get("telegram.unknown_command") + "\n\n" + texts.get("telegram.help")));
        };
    }

    private List<Reply> undo() {
        return switch (undoLastEntry.undoLast()) {
            case UndoResult.Undone undone -> List.of(Reply.of(texts.get("telegram.undone", entries.describe(undone.entry()))));
            case UndoResult.NothingToUndo ignored -> reply("telegram.nothing_to_undo");
        };
    }

    private List<Reply> reply(String key) {
        return List.of(Reply.of(texts.get(key)));
    }
}
