package io.github.marcosaalbanojunior.prontuario.config;

import io.github.marcosaalbanojunior.prontuario.adapter.in.telegram.PollingRunner;
import io.github.marcosaalbanojunior.prontuario.adapter.out.client.telegram.TelegramApiHints;
import io.github.marcosaalbanojunior.prontuario.adapter.out.client.telegram.TelegramGatewayAdapter;
import io.github.marcosaalbanojunior.prontuario.adapter.out.client.telegram.TelegramRestClients;
import io.github.marcosaalbanojunior.prontuario.adapter.out.persistence.UpdateOffsetRepositoryAdapter;
import io.github.marcosaalbanojunior.prontuario.application.port.in.GetGoalProgressUseCase;
import io.github.marcosaalbanojunior.prontuario.application.port.in.HandleChatMessageUseCase;
import io.github.marcosaalbanojunior.prontuario.application.port.in.PollUpdatesUseCase;
import io.github.marcosaalbanojunior.prontuario.application.port.in.entry.RecordQuickEntryUseCase;
import io.github.marcosaalbanojunior.prontuario.application.port.in.entry.UndoLastEntryUseCase;
import io.github.marcosaalbanojunior.prontuario.application.port.out.MessagesPort;
import io.github.marcosaalbanojunior.prontuario.application.port.out.TransactionPort;
import io.github.marcosaalbanojunior.prontuario.application.port.out.telegram.TelegramGatewayPort;
import io.github.marcosaalbanojunior.prontuario.application.usecase.telegram.HandleChatMessageService;
import io.github.marcosaalbanojunior.prontuario.application.usecase.telegram.PollUpdatesService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.jdbc.core.simple.JdbcClient;

@Configuration(proxyBeanMethods = false)
@ImportRuntimeHints(TelegramApiHints.class)
class TelegramConfig {

    @Bean
    TelegramGatewayPort telegramGateway(TelegramProperties telegram) {
        return new TelegramGatewayAdapter(
                TelegramRestClients.create(telegram.apiUrl(), telegram.botToken(), PollUpdatesService.LONG_POLL));
    }

    @Bean
    HandleChatMessageUseCase handleChatMessage(RecordQuickEntryUseCase recordQuickEntry, UndoLastEntryUseCase undoLastEntry,
                                               GetGoalProgressUseCase goalProgress, MessagesPort messages, Clock clock,
                                               AppProperties app) {
        return new HandleChatMessageService(recordQuickEntry, undoLastEntry, goalProgress, messages, clock, app.timezone());
    }

    @Bean
    PollUpdatesUseCase pollUpdates(TelegramGatewayPort telegramGateway, JdbcClient jdbc, HandleChatMessageUseCase chat,
                                   MessagesPort messages, TransactionPort transactions, TelegramProperties telegram) {
        return new PollUpdatesService(telegramGateway, new UpdateOffsetRepositoryAdapter(jdbc), chat, messages, transactions,
                telegram.allowedChatId());
    }

    @Bean
    PollingRunner pollingRunner(PollUpdatesUseCase pollUpdates, TelegramProperties telegram) {
        return new PollingRunner(pollUpdates, telegram.polling());
    }
}
