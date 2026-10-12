package io.github.marcosaalbanojunior.prontuario.domain.service.quickentry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.marcosaalbanojunior.prontuario.domain.model.entry.BodyRegion;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("BodyRegionVocabulary")
class BodyRegionVocabularyTest {

    @Nested
    @DisplayName("find(...)")
    class Find {

        private final BodyRegionVocabulary vocabulary =
                new BodyRegionVocabulary(Map.of(BodyRegion.ABDOMEN, List.of("abdômen", "barriga")));

        @ParameterizedTest(name = "\"{0}\" is the abdomen")
        @ValueSource(strings = {"abdômen", "  ABDOMEN ", "barriga", "Barriga"})
        @DisplayName("matches ignoring accents, case and extra spaces")
        void should_findRegion_when_wordsMatchLoosely(String words) {
            assertThat(vocabulary.find(words)).contains(BodyRegion.ABDOMEN);
        }

        @Test
        @DisplayName("finds nothing for unknown words")
        void should_beEmpty_when_wordsAreUnknown() {
            assertThat(vocabulary.find("pé")).isEmpty();
        }
    }

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        @DisplayName("accepts the same alias repeated for one region")
        void should_accept_when_aliasRepeatsInTheSameRegion() {
            var vocabulary = new BodyRegionVocabulary(Map.of(BodyRegion.HEAD, List.of("cabeça", "Cabeca")));

            assertThat(vocabulary.find("cabeca")).contains(BodyRegion.HEAD);
        }

        @Test
        @DisplayName("rejects an alias used for two regions")
        void should_reject_when_aliasBelongsToTwoRegions() {
            var aliases = Map.of(BodyRegion.LEFT_LEG, List.of("perna"), BodyRegion.RIGHT_LEG, List.of("Perna"));

            assertThatThrownBy(() -> new BodyRegionVocabulary(aliases)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
