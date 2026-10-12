package io.github.marcosaalbanojunior.prontuario.adapter.out.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.marcosaalbanojunior.prontuario.support.PtBrMessages;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MessageSourceAdapter.get(...)")
class MessageSourceAdapterTest {

    @Test
    @DisplayName("returns the pt-BR text with accents")
    void should_returnPortugueseText_when_keyHasNoArguments() {
        assertThat(PtBrMessages.load().get("app.name")).isEqualTo("Prontuário Único");
    }

    @Test
    @DisplayName("fills in the arguments")
    void should_fillArguments_when_keyHasPlaceholders() {
        assertThat(PtBrMessages.load().get("entry.water", "500 mL", "14:32")).isEqualTo("Água: 500 mL às 14:32");
    }
}
