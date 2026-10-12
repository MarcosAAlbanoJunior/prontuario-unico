package io.github.marcosaalbanojunior.prontuario.goals.config;

import io.github.marcosaalbanojunior.prontuario.common.config.AppProperties;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.SumConfirmedEvents;
import io.github.marcosaalbanojunior.prontuario.goals.adapter.out.entries.EntriesConfirmedTotals;
import io.github.marcosaalbanojunior.prontuario.goals.adapter.out.persistence.JdbcGoalRepository;
import io.github.marcosaalbanojunior.prontuario.goals.application.port.in.GetGoalProgress;
import io.github.marcosaalbanojunior.prontuario.goals.application.service.GetGoalProgressService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

@Configuration(proxyBeanMethods = false)
class GoalsConfig {

    @Bean
    GetGoalProgress getGoalProgress(JdbcClient jdbc, SumConfirmedEvents entries, Clock clock, AppProperties app) {
        return new GetGoalProgressService(new JdbcGoalRepository(jdbc), new EntriesConfirmedTotals(entries), clock,
                app.timezone());
    }
}
