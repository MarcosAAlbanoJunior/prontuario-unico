package io.github.marcosaalbanojunior.prontuario.domain.model.goal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.marcosaalbanojunior.prontuario.domain.model.entry.EventType;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Goal and GoalProgress")
class GoalProgressTest {

    @Nested
    @DisplayName("GoalProgress")
    class Progress {

        @ParameterizedTest(name = "{0} of 2000 is {1}%, met={2}")
        @CsvSource({
            "0,    0,   false",
            "1999, 99,  false",
            "2000, 100, true",
            "2500, 125, true",
        })
        @DisplayName("reports a whole, uncapped percentage and whether the goal is met")
        void should_reportPercentAndMet_when_comparingWithTarget(BigDecimal achieved, int percent, boolean met) {
            var progress = new GoalProgress(water(2000), achieved);

            assertThat(progress.percent()).isEqualTo(percent);
            assertThat(progress.isMet()).isEqualTo(met);
        }
    }

    @Nested
    @DisplayName("Goal")
    class Goals {

        @ParameterizedTest(name = "rejects target {0}")
        @ValueSource(ints = {0, -5})
        @DisplayName("rejects a target that is not positive")
        void should_reject_when_targetIsNotPositive(int target) {
            assertThatThrownBy(() -> water(target)).isInstanceOf(InvalidGoalException.class);
        }
    }

    @Nested
    @DisplayName("GoalType.measuredBy()")
    class MeasuredBy {

        @ParameterizedTest(name = "{0} is measured by {1} events")
        @CsvSource({"WATER, WATER", "SLEEP, SLEEP", "EXERCISE, EXERCISE"})
        @DisplayName("maps measurable goals to the events that add up to them")
        void should_returnEventType_when_goalIsMeasurable(GoalType type, EventType eventType) {
            assertThat(type.measuredBy()).contains(eventType);
        }

        @Test
        @DisplayName("has nothing that measures custom goals")
        void should_beEmpty_when_goalIsCustom() {
            assertThat(GoalType.CUSTOM.measuredBy()).isEqualTo(Optional.empty());
        }
    }

    private static Goal water(int target) {
        return new Goal(UUID.randomUUID(), GoalType.WATER, BigDecimal.valueOf(target), "ml", GoalPeriod.DAY, true);
    }
}
