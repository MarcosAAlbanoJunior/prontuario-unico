package io.github.marcosaalbanojunior.prontuario.telegram.adapter.out.chat;

import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.RecordQuickEntry;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.UndoLastEntry;
import io.github.marcosaalbanojunior.prontuario.goals.application.port.in.GetGoalProgress;

/** The other modules' inbound ports the chat uses. */
public record ChatPorts(RecordQuickEntry recordQuickEntry, UndoLastEntry undoLastEntry, GetGoalProgress goalProgress) {}
