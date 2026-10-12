package io.github.marcosaalbanojunior.prontuario.config;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.context.support.ResourceBundleMessageSource;

// Boot only auto-configures a MessageSource when a base messages.properties exists;
// pt-BR is the only UI language for now, so it is declared here as the default.
@Configuration(proxyBeanMethods = false)
@ImportRuntimeHints(I18nConfig.MessagesHints.class)
class I18nConfig {

    static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    @Bean
    MessageSource messageSource() {
        var source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding(StandardCharsets.UTF_8.name());
        source.setDefaultLocale(PT_BR);
        source.setFallbackToSystemLocale(false);
        return source;
    }

    static class MessagesHints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            hints.resources().registerPattern("messages*.properties");
        }
    }
}
