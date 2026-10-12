package io.github.marcosaalbanojunior.prontuario.entries.config;

import io.github.marcosaalbanojunior.prontuario.common.config.AppProperties;
import io.github.marcosaalbanojunior.prontuario.common.id.IdGenerator;
import io.github.marcosaalbanojunior.prontuario.common.tx.TransactionRunner;
import io.github.marcosaalbanojunior.prontuario.entries.adapter.out.persistence.JdbcEventRepository;
import io.github.marcosaalbanojunior.prontuario.entries.adapter.out.persistence.JdbcSourceRepository;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.RecordQuickEntry;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.SumConfirmedEvents;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.UndoLastEntry;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.out.EventRepository;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.out.SourceRepository;
import io.github.marcosaalbanojunior.prontuario.entries.application.service.RecordQuickEntryService;
import io.github.marcosaalbanojunior.prontuario.entries.application.service.SumConfirmedEventsService;
import io.github.marcosaalbanojunior.prontuario.entries.application.service.UndoLastEntryService;
import io.github.marcosaalbanojunior.prontuario.entries.domain.quick.BodyRegionVocabulary;
import io.github.marcosaalbanojunior.prontuario.entries.domain.quick.QuickEntryParser;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

@Configuration(proxyBeanMethods = false)
class EntriesConfig {

    @Bean
    EventRepository eventRepository(JdbcClient jdbc) {
        return new JdbcEventRepository(jdbc);
    }

    @Bean
    SourceRepository sourceRepository(JdbcClient jdbc) {
        return new JdbcSourceRepository(jdbc);
    }

    @Bean
    RecordQuickEntry recordQuickEntry(BodyRegionVocabulary vocabulary, EventRepository events, SourceRepository sources,
                                      IdGenerator ids, TransactionRunner transactions, Clock clock, AppProperties app) {
        return new RecordQuickEntryService(new QuickEntryParser(vocabulary), events, sources, ids, transactions, clock,
                app.timezone());
    }

    @Bean
    UndoLastEntry undoLastEntry(EventRepository events, TransactionRunner transactions) {
        return new UndoLastEntryService(events, transactions);
    }

    @Bean
    SumConfirmedEvents sumConfirmedEvents(EventRepository events) {
        return new SumConfirmedEventsService(events);
    }
}
