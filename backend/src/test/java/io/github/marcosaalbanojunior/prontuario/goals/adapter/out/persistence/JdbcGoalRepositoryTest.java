package io.github.marcosaalbanojunior.prontuario.goals.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.marcosaalbanojunior.prontuario.TestcontainersConfiguration;
import io.github.marcosaalbanojunior.prontuario.goals.domain.Goal;
import io.github.marcosaalbanojunior.prontuario.goals.domain.GoalPeriod;
import io.github.marcosaalbanojunior.prontuario.goals.domain.GoalType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

@DisabledInNativeImage
@JdbcTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class JdbcGoalRepositoryTest {

    private final JdbcClient jdbc;
    private final JdbcGoalRepository goals;

    JdbcGoalRepositoryTest(JdbcClient jdbc) {
        this.jdbc = jdbc;
        this.goals = new JdbcGoalRepository(jdbc);
    }

    @Test
    void shouldStartWithTheDefaultGoals() {
        assertThat(goals.activeGoals())
                .extracting(Goal::type, goal -> goal.target().intValueExact(), Goal::unit, Goal::period)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(GoalType.WATER, 2000, "ml", GoalPeriod.DAY),
                        org.assertj.core.groups.Tuple.tuple(GoalType.SLEEP, 420, "min", GoalPeriod.DAY),
                        org.assertj.core.groups.Tuple.tuple(GoalType.EXERCISE, 150, "min", GoalPeriod.WEEK));
    }

    @Test
    void shouldIgnoreInactiveGoals() {
        jdbc.sql("UPDATE goal SET active = false WHERE type = 'sleep'").update();

        assertThat(goals.activeGoals()).extracting(Goal::type).doesNotContain(GoalType.SLEEP);
    }
}
