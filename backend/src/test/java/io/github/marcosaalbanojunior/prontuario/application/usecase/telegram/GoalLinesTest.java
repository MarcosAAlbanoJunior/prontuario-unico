package io.github.marcosaalbanojunior.prontuario.application.usecase.telegram;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.marcosaalbanojunior.prontuario.domain.model.goal.Goal;
import io.github.marcosaalbanojunior.prontuario.domain.model.goal.GoalPeriod;
import io.github.marcosaalbanojunior.prontuario.domain.model.goal.GoalProgress;
import io.github.marcosaalbanojunior.prontuario.domain.model.goal.GoalType;
import io.github.marcosaalbanojunior.prontuario.support.PtBrMessages;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GoalLines.today(...)")
class GoalLinesTest {

    private final GoalLines lines = new GoalLines(PtBrMessages.load(),
            new PtBrFormat(Clock.fixed(Instant.parse("2026-10-12T17:00:00Z"), ZoneId.of("UTC")), ZoneId.of("America/Sao_Paulo")));

    @Test
    @DisplayName("lists today's goals, then this week's, marking the ones met")
    void should_listDayThenWeek_when_bothExist() {
        var progress = List.of(
                progress(GoalType.WATER, 2000, "ml", GoalPeriod.DAY, 1500),
                progress(GoalType.SLEEP, 420, "min", GoalPeriod.DAY, 400),
                progress(GoalType.EXERCISE, 150, "min", GoalPeriod.WEEK, 160));

        assertThat(lines.today(progress)).isEqualTo("""
                Hoje
                Água: 1.500 mL de 2.000 mL (75%)
                Sono: 6 h 40 min de 7 h (95%)

                Nesta semana
                Treino: 2 h 40 min de 2 h 30 min (106%) ✅""");
    }

    @Test
    @DisplayName("omits a period without goals")
    void should_omitSection_when_periodHasNoGoals() {
        assertThat(lines.today(List.of(progress(GoalType.EXERCISE, 150, "min", GoalPeriod.WEEK, 0))))
                .isEqualTo("Nesta semana\nTreino: 0 min de 2 h 30 min (0%)");
    }

    @Test
    @DisplayName("says so when there are no active goals")
    void should_sayNoGoals_when_listIsEmpty() {
        assertThat(lines.today(List.of())).isEqualTo("Nenhuma meta ativa.");
    }

    private static GoalProgress progress(GoalType type, int target, String unit, GoalPeriod period, int achieved) {
        return new GoalProgress(new Goal(UUID.randomUUID(), type, BigDecimal.valueOf(target), unit, period, true),
                BigDecimal.valueOf(achieved));
    }
}
