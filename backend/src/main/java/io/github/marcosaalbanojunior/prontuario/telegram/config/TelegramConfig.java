package io.github.marcosaalbanojunior.prontuario.telegram.config;

import io.github.marcosaalbanojunior.prontuario.common.config.AppProperties;
import io.github.marcosaalbanojunior.prontuario.common.tx.TransactionRunner;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.RecordQuickEntry;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.UndoLastEntry;
import io.github.marcosaalbanojunior.prontuario.goals.application.port.in.GetGoalProgress;
import io.github.marcosaalbanojunior.prontuario.telegram.adapter.in.polling.PollingRunner;
import io.github.marcosaalbanojunior.prontuario.telegram.adapter.out.chat.ChatPorts;
import io.github.marcosaalbanojunior.prontuario.telegram.adapter.out.chat.CommandRouter;
import io.github.marcosaalbanojunior.prontuario.telegram.adapter.out.chat.PtBrFormat;
import io.github.marcosaalbanojunior.prontuario.telegram.adapter.out.chat.Texts;
import io.github.marcosaalbanojunior.prontuario.telegram.adapter.out.http.TelegramApiHints;
import io.github.marcosaalbanojunior.prontuario.telegram.adapter.out.http.TelegramHttpGateway;
import io.github.marcosaalbanojunior.prontuario.telegram.adapter.out.http.TelegramRestClients;
import io.github.marcosaalbanojunior.prontuario.telegram.adapter.out.persistence.JdbcUpdateOffsetStore;
import io.github.marcosaalbanojunior.prontuario.telegram.application.port.in.PollUpdates;
import io.github.marcosaalbanojunior.prontuario.telegram.application.port.out.ChatResponder;
import io.github.marcosaalbanojunior.prontuario.telegram.application.port.out.TelegramGateway;
import io.github.marcosaalbanojunior.prontuario.telegram.application.service.PollUpdatesService;
import java.time.Clock;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.jdbc.core.simple.JdbcClient;

@Configuration(proxyBeanMethods = false)
@ImportRuntimeHints(TelegramApiHints.class)
class TelegramConfig {

    @Bean
    TelegramGateway telegramGateway(TelegramProperties telegram) {
        return new TelegramHttpGateway(
                TelegramRestClients.create(telegram.apiUrl(), telegram.botToken(), PollUpdatesService.LONG_POLL));
    }

    @Bean
    ChatResponder chatResponder(RecordQuickEntry recordQuickEntry, UndoLastEntry undoLastEntry, GetGoalProgress goalProgress,
                                MessageSource messages, Clock clock, AppProperties app) {
        return new CommandRouter(new ChatPorts(recordQuickEntry, undoLastEntry, goalProgress), new Texts(messages),
                new PtBrFormat(clock, app.timezone()));
    }

    @Bean
    PollUpdates pollUpdates(TelegramGateway telegramGateway, JdbcClient jdbc, ChatResponder chatResponder,
                            TransactionRunner transactions, TelegramProperties telegram) {
        return new PollUpdatesService(telegramGateway, new JdbcUpdateOffsetStore(jdbc), chatResponder, transactions,
                telegram.allowedChatId());
    }

    @Bean
    PollingRunner pollingRunner(PollUpdates pollUpdates, TelegramProperties telegram) {
        return new PollingRunner(pollUpdates, telegram.polling());
    }
}
