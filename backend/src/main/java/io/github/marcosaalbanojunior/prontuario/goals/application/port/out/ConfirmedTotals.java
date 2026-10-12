package io.github.marcosaalbanojunior.prontuario.goals.application.port.out;

import io.github.marcosaalbanojunior.prontuario.goals.domain.GoalType;
import io.github.marcosaalbanojunior.prontuario.goals.domain.PeriodWindow;
import java.math.BigDecimal;

/** How much was recorded for a measurable goal type in a period, in the goal's unit. */
public interface ConfirmedTotals {

    BigDecimal total(GoalType type, PeriodWindow window);
}
