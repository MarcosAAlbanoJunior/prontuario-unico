package io.github.marcosaalbanojunior.prontuario.config;

import io.github.marcosaalbanojunior.prontuario.adapter.out.i18n.MessageSourceAdapter;
import io.github.marcosaalbanojunior.prontuario.adapter.out.id.UuidV7IdGeneratorAdapter;
import io.github.marcosaalbanojunior.prontuario.adapter.out.persistence.TransactionAdapter;
import io.github.marcosaalbanojunior.prontuario.application.port.out.IdGeneratorPort;
import io.github.marcosaalbanojunior.prontuario.application.port.out.MessagesPort;
import io.github.marcosaalbanojunior.prontuario.application.port.out.TransactionPort;
import java.security.SecureRandom;
import java.time.Clock;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Shared infrastructure every use case relies on. */
@Configuration(proxyBeanMethods = false)
class CoreConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    IdGeneratorPort idGenerator(Clock clock) {
        return new UuidV7IdGeneratorAdapter(clock, new SecureRandom());
    }

    @Bean
    TransactionPort transactions(PlatformTransactionManager transactionManager) {
        return new TransactionAdapter(new TransactionTemplate(transactionManager));
    }

    @Bean
    MessagesPort messages(MessageSource messageSource) {
        return new MessageSourceAdapter(messageSource);
    }
}
