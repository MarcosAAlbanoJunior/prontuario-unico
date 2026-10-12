package io.github.marcosaalbanojunior.prontuario.telegram.adapter.out.chat;

import io.github.marcosaalbanojunior.prontuario.goals.application.port.in.GoalProgressView;
import java.util.ArrayList;
import java.util.List;

/** The /hoje answer: today's goals, then this week's. */
final class GoalLines {

    private final Texts texts;
    private final PtBrFormat format;

    GoalLines(Texts texts, PtBrFormat format) {
        this.texts = texts;
        this.format = format;
    }

    String today(List<GoalProgressView> progress) {
        if (progress.isEmpty()) {
            return texts.get("telegram.today.none");
        }
        var lines = new ArrayList<String>();
        section(lines, "telegram.today.day", progress, "day");
        section(lines, "telegram.today.week", progress, "week");
        return String.join("\n", lines).strip();
    }

    private void section(List<String> lines, String titleKey, List<GoalProgressView> progress, String period) {
        var goals = progress.stream().filter(goal -> goal.period().equals(period)).toList();
        if (goals.isEmpty()) {
            return;
        }
        lines.add(texts.get(titleKey));
        goals.forEach(goal -> lines.add(line(goal)));
        lines.add("");
    }

    private String line(GoalProgressView goal) {
        return texts.get("telegram.today.line",
                texts.get("goal." + goal.type()),
                format.quantity(goal.achieved(), goal.unit()),
                format.quantity(goal.target(), goal.unit()),
                goal.percent(),
                goal.met() ? texts.get("telegram.today.met") : "");
    }
}
