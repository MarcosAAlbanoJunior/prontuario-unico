package io.github.marcosaalbanojunior.prontuario.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import io.github.marcosaalbanojunior.prontuario.domain.model.goal.Goal;
import io.github.marcosaalbanojunior.prontuario.domain.model.goal.GoalPeriod;
import io.github.marcosaalbanojunior.prontuario.domain.model.goal.GoalType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

@PostgresTest
@DisplayName("GoalRepositoryAdapter.activeGoals()")
class GoalRepositoryAdapterTest {

    private final JdbcClient jdbc;
    private final GoalRepositoryAdapter goals;

    GoalRepositoryAdapterTest(JdbcClient jdbc) {
        this.jdbc = jdbc;
        this.goals = new GoalRepositoryAdapter(jdbc);
    }

    @Test
    @DisplayName("starts with the default goals created by the migrations")
    void should_returnDefaultGoals_when_databaseIsNew() {
        assertThat(goals.activeGoals())
                .extracting(Goal::type, goal -> goal.target().intValueExact(), Goal::unit, Goal::period, Goal::active)
                .containsExactlyInAnyOrder(
                        tuple(GoalType.WATER, 2000, "ml", GoalPeriod.DAY, true),
                        tuple(GoalType.SLEEP, 420, "min", GoalPeriod.DAY, true),
                        tuple(GoalType.EXERCISE, 150, "min", GoalPeriod.WEEK, true));
    }

    @Test
    @DisplayName("leaves inactive goals out")
    void should_skipInactiveGoals_when_listing() {
        jdbc.sql("UPDATE goal SET active = false WHERE type = 'sleep'").update();

        assertThat(goals.activeGoals()).extracting(Goal::type).doesNotContain(GoalType.SLEEP);
    }
}
