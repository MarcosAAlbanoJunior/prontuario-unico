package io.github.marcosaalbanojunior.prontuario.goals.adapter.out.persistence;

import io.github.marcosaalbanojunior.prontuario.goals.application.port.out.GoalRepository;
import io.github.marcosaalbanojunior.prontuario.goals.domain.Goal;
import io.github.marcosaalbanojunior.prontuario.goals.domain.GoalPeriod;
import io.github.marcosaalbanojunior.prontuario.goals.domain.GoalType;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

public final class JdbcGoalRepository implements GoalRepository {

    private final JdbcClient jdbc;

    public JdbcGoalRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Goal> activeGoals() {
        return jdbc.sql("SELECT id, type, target_value, unit, period, active FROM goal WHERE active ORDER BY period, type")
                .query((row, rowNumber) -> new Goal(
                        row.getObject("id", UUID.class),
                        GoalType.valueOf(row.getString("type").toUpperCase(Locale.ROOT)),
                        row.getBigDecimal("target_value"),
                        row.getString("unit"),
                        GoalPeriod.valueOf(row.getString("period").toUpperCase(Locale.ROOT)),
                        row.getBoolean("active")))
                .list();
    }
}
