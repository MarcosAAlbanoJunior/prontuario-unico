package io.github.marcosaalbanojunior.prontuario.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

// Also runs in native:test, which checks the resource hint for the message files.
@SpringJUnitConfig(I18nConfig.class)
@DisplayName("I18nConfig message source")
class I18nConfigTest {

    private final MessageSource messages;

    I18nConfigTest(MessageSource messages) {
        this.messages = messages;
    }

    @Test
    @DisplayName("resolves pt-BR text with accents")
    void should_resolvePortuguese_when_askedInPortuguese() {
        assertThat(messages.getMessage("app.name", null, I18nConfig.PT_BR)).isEqualTo("Prontuário Único");
    }

    @Test
    @DisplayName("falls back to pt-BR for any other locale")
    void should_fallBackToPortuguese_when_askedInAnotherLocale() {
        assertThat(messages.getMessage("app.name", null, Locale.ENGLISH)).isEqualTo("Prontuário Único");
    }
}
