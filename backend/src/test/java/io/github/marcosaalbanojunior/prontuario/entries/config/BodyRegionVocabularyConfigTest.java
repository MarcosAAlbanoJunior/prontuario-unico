package io.github.marcosaalbanojunior.prontuario.entries.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.marcosaalbanojunior.prontuario.entries.domain.BodyRegion;
import io.github.marcosaalbanojunior.prontuario.entries.domain.quick.BodyRegionVocabulary;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

// Also runs in native:test, which checks the resource hint for the vocabulary file.
@SpringJUnitConfig(BodyRegionVocabularyConfig.class)
class BodyRegionVocabularyConfigTest {

    private final BodyRegionVocabulary vocabulary;

    BodyRegionVocabularyConfigTest(BodyRegionVocabulary vocabulary) {
        this.vocabulary = vocabulary;
    }

    @ParameterizedTest
    @CsvSource({
        "cabeça, HEAD", "nuca, NECK", "peito, CHEST", "barriga, ABDOMEN", "costas, UPPER_BACK", "lombar, LOWER_BACK",
        "braço esquerdo, LEFT_ARM", "ombro direito, RIGHT_ARM", "joelho esquerdo, LEFT_KNEE", "joelho direito, RIGHT_KNEE",
        "perna esquerda, LEFT_LEG", "panturrilha direita, RIGHT_LEG", "corpo todo, SYSTEMIC",
    })
    void shouldRecognizeEveryBodyRegionInPortuguese(String words, BodyRegion region) {
        assertThat(vocabulary.find(words)).contains(region);
    }
}
