package io.github.marcosaalbanojunior.prontuario.config;

import io.github.marcosaalbanojunior.prontuario.adapter.out.persistence.GoalRepositoryAdapter;
import io.github.marcosaalbanojunior.prontuario.application.port.in.GetGoalProgressUseCase;
import io.github.marcosaalbanojunior.prontuario.application.port.out.EventRepositoryPort;
import io.github.marcosaalbanojunior.prontuario.application.usecase.GetGoalProgressService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

@Configuration(proxyBeanMethods = false)
class GoalConfig {

    @Bean
    GetGoalProgressUseCase getGoalProgress(JdbcClient jdbc, EventRepositoryPort events, Clock clock, AppProperties app) {
        return new GetGoalProgressService(new GoalRepositoryAdapter(jdbc), events, clock, app.timezone());
    }
}
