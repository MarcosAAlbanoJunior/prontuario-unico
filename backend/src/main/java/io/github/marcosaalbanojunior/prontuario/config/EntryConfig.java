package io.github.marcosaalbanojunior.prontuario.config;

import io.github.marcosaalbanojunior.prontuario.adapter.out.persistence.entry.EventRepositoryAdapter;
import io.github.marcosaalbanojunior.prontuario.adapter.out.persistence.entry.SourceRepositoryAdapter;
import io.github.marcosaalbanojunior.prontuario.application.port.in.entry.RecordQuickEntryUseCase;
import io.github.marcosaalbanojunior.prontuario.application.port.in.entry.UndoLastEntryUseCase;
import io.github.marcosaalbanojunior.prontuario.application.port.out.EventRepositoryPort;
import io.github.marcosaalbanojunior.prontuario.application.port.out.IdGeneratorPort;
import io.github.marcosaalbanojunior.prontuario.application.port.out.SourceRepositoryPort;
import io.github.marcosaalbanojunior.prontuario.application.port.out.TransactionPort;
import io.github.marcosaalbanojunior.prontuario.application.usecase.entry.RecordQuickEntryService;
import io.github.marcosaalbanojunior.prontuario.application.usecase.entry.UndoLastEntryService;
import io.github.marcosaalbanojunior.prontuario.domain.service.quickentry.BodyRegionVocabulary;
import io.github.marcosaalbanojunior.prontuario.domain.service.quickentry.QuickEntryParser;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

@Configuration(proxyBeanMethods = false)
class EntryConfig {

    @Bean
    EventRepositoryPort eventRepository(JdbcClient jdbc) {
        return new EventRepositoryAdapter(jdbc);
    }

    @Bean
    SourceRepositoryPort sourceRepository(JdbcClient jdbc) {
        return new SourceRepositoryAdapter(jdbc);
    }

    @Bean
    RecordQuickEntryUseCase recordQuickEntry(BodyRegionVocabulary vocabulary, EventRepositoryPort events,
                                             SourceRepositoryPort sources, IdGeneratorPort ids,
                                             TransactionPort transactions, Clock clock, AppProperties app) {
        return new RecordQuickEntryService(new QuickEntryParser(vocabulary), events, sources, ids, transactions, clock,
                app.timezone());
    }

    @Bean
    UndoLastEntryUseCase undoLastEntry(EventRepositoryPort events, TransactionPort transactions) {
        return new UndoLastEntryService(events, transactions);
    }
}
