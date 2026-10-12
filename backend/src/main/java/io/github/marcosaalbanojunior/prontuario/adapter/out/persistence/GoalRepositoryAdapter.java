package io.github.marcosaalbanojunior.prontuario.adapter.out.persistence;

import io.github.marcosaalbanojunior.prontuario.application.port.out.GoalRepositoryPort;
import io.github.marcosaalbanojunior.prontuario.domain.model.goal.Goal;
import io.github.marcosaalbanojunior.prontuario.domain.model.goal.GoalPeriod;
import io.github.marcosaalbanojunior.prontuario.domain.model.goal.GoalType;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

public final class GoalRepositoryAdapter implements GoalRepositoryPort {

    private final JdbcClient jdbc;

    public GoalRepositoryAdapter(JdbcClient jdbc) {
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
