package io.github.marcosaalbanojunior.prontuario.common.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(I18nConfig.class)
class I18nConfigTest {

    private final MessageSource messages;

    I18nConfigTest(MessageSource messages) {
        this.messages = messages;
    }

    @Test
    void shouldResolvePortugueseTextWithAccents() {
        assertThat(messages.getMessage("app.name", null, I18nConfig.PT_BR)).isEqualTo("Prontuário Único");
    }

    @Test
    void shouldFallBackToPortugueseForOtherLocales() {
        assertThat(messages.getMessage("app.name", null, Locale.ENGLISH)).isEqualTo("Prontuário Único");
    }
}
