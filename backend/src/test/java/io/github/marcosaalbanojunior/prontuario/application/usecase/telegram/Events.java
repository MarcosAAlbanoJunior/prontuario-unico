package io.github.marcosaalbanojunior.prontuario.application.usecase.telegram;

import io.github.marcosaalbanojunior.prontuario.domain.model.entry.CreatedVia;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.Event;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.EventDetails;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.EventType;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.MeasureUnit;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.Measurement;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.TimePrecision;
import io.github.marcosaalbanojunior.prontuario.domain.model.goal.Goal;
import io.github.marcosaalbanojunior.prontuario.domain.model.goal.GoalPeriod;
import io.github.marcosaalbanojunior.prontuario.domain.model.goal.GoalProgress;
import io.github.marcosaalbanojunior.prontuario.domain.model.goal.GoalType;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/** Events and goals for the telegram use case tests, at 14:32 in São Paulo. */
final class Events {

    private Events() {}

    static Event water(int milliliters) {
        return event(new EventDetails(EventType.WATER, Optional.empty(), Optional.empty(),
                Optional.of(new Measurement(BigDecimal.valueOf(milliliters), MeasureUnit.MILLILITER)), Optional.empty(),
                HandleChatMessageServiceTest.NOW, Optional.empty(), TimePrecision.EXACT));
    }

    static Event event(EventDetails details) {
        return Event.confirmed(UUID.randomUUID(), details, UUID.randomUUID(), CreatedVia.COMMAND, details.occurredStart());
    }

    static GoalProgress waterGoalProgress(int achieved) {
        var goal = new Goal(UUID.randomUUID(), GoalType.WATER, BigDecimal.valueOf(2000), "ml", GoalPeriod.DAY, true);
        return new GoalProgress(goal, BigDecimal.valueOf(achieved));
    }
}
