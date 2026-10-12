package io.github.marcosaalbanojunior.prontuario.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.marcosaalbanojunior.prontuario.domain.model.entry.BodyRegion;
import io.github.marcosaalbanojunior.prontuario.domain.service.quickentry.BodyRegionVocabulary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

// Also runs in native:test, which checks the resource hint for the vocabulary file.
@SpringJUnitConfig(BodyRegionVocabularyConfig.class)
@DisplayName("BodyRegionVocabularyConfig")
class BodyRegionVocabularyConfigTest {

    private final BodyRegionVocabulary vocabulary;

    BodyRegionVocabularyConfigTest(BodyRegionVocabulary vocabulary) {
        this.vocabulary = vocabulary;
    }

    @ParameterizedTest(name = "\"{0}\" is {1}")
    @CsvSource({
        "cabeça, HEAD", "nuca, NECK", "peito, CHEST", "barriga, ABDOMEN", "costas, UPPER_BACK", "lombar, LOWER_BACK",
        "braço esquerdo, LEFT_ARM", "ombro direito, RIGHT_ARM", "joelho esquerdo, LEFT_KNEE", "joelho direito, RIGHT_KNEE",
        "perna esquerda, LEFT_LEG", "panturrilha direita, RIGHT_LEG", "corpo todo, SYSTEMIC",
    })
    @DisplayName("recognizes every body region in Portuguese")
    void should_recognizeRegion_when_wordsArePortuguese(String words, BodyRegion region) {
        assertThat(vocabulary.find(words)).contains(region);
    }
}
