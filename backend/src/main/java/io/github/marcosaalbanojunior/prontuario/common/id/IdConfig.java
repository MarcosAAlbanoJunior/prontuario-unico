package io.github.marcosaalbanojunior.prontuario.common.id;

import java.security.SecureRandom;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class IdConfig {

    @Bean
    IdGenerator idGenerator(Clock clock) {
        return new UuidV7Generator(clock, new SecureRandom());
    }
}
