package io.github.marcosaalbanojunior.prontuario.application.usecase.telegram;

import io.github.marcosaalbanojunior.prontuario.application.port.out.MessagesPort;
import io.github.marcosaalbanojunior.prontuario.domain.model.goal.GoalPeriod;
import io.github.marcosaalbanojunior.prontuario.domain.model.goal.GoalProgress;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

/** The /hoje answer: today's goals, then this week's. */
@RequiredArgsConstructor
final class GoalLines {

    private final MessagesPort messages;
    private final PtBrFormat format;

    String today(List<GoalProgress> progress) {
        if (progress.isEmpty()) {
            return messages.get("telegram.today.none");
        }
        var lines = new ArrayList<String>();
        section(lines, "telegram.today.day", progress, GoalPeriod.DAY);
        section(lines, "telegram.today.week", progress, GoalPeriod.WEEK);
        return String.join("\n", lines).strip();
    }

    private void section(List<String> lines, String titleKey, List<GoalProgress> progress, GoalPeriod period) {
        var goals = progress.stream().filter(goal -> goal.goal().period() == period).toList();
        if (goals.isEmpty()) {
            return;
        }
        lines.add(messages.get(titleKey));
        goals.forEach(goal -> lines.add(line(goal)));
        lines.add("");
    }

    private String line(GoalProgress progress) {
        var goal = progress.goal();
        return messages.get("telegram.today.line",
                messages.get("goal." + MessageKeys.of(goal.type())),
                format.quantity(progress.achieved(), goal.unit()),
                format.quantity(goal.target(), goal.unit()),
                progress.percent(),
                progress.isMet() ? messages.get("telegram.today.met") : "");
    }
}
