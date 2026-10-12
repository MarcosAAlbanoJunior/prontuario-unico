package io.github.marcosaalbanojunior.prontuario.goals.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GoalProgressTest {

    @ParameterizedTest(name = "{0} of 2000 -> {1}% met={2}")
    @CsvSource({
        "0,    0,   false",
        "1999, 99,  false",
        "2000, 100, true",
        "2500, 125, true",
    })
    void shouldReportPercentWithoutCappingAndWhetherTheGoalIsMet(BigDecimal achieved, int percent, boolean met) {
        var progress = new GoalProgress(water(2000), achieved);

        assertThat(progress.percent()).isEqualTo(percent);
        assertThat(progress.isMet()).isEqualTo(met);
    }

    @Test
    void shouldRejectGoalWithoutPositiveTarget() {
        assertThatThrownBy(() -> water(0)).isInstanceOf(InvalidGoalException.class);
    }

    private static Goal water(int target) {
        return new Goal(UUID.randomUUID(), GoalType.WATER, BigDecimal.valueOf(target), "ml", GoalPeriod.DAY, true);
    }
}
