package io.github.marcosaalbanojunior.prontuario.entries.domain.quick;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.marcosaalbanojunior.prontuario.entries.domain.BodyRegion;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BodyRegionVocabularyTest {

    @Test
    void shouldMatchIgnoringAccentsCaseAndExtraSpaces() {
        var vocabulary = new BodyRegionVocabulary(Map.of(BodyRegion.ABDOMEN, List.of("abdômen", "barriga")));

        assertThat(vocabulary.find("  ABDOMEN ")).contains(BodyRegion.ABDOMEN);
        assertThat(vocabulary.find("barriga")).contains(BodyRegion.ABDOMEN);
        assertThat(vocabulary.find("pé")).isEmpty();
    }

    @Test
    void shouldRejectAnAliasUsedForTwoRegions() {
        var aliases = Map.of(BodyRegion.LEFT_LEG, List.of("perna"), BodyRegion.RIGHT_LEG, List.of("Perna"));

        assertThatThrownBy(() -> new BodyRegionVocabulary(aliases)).isInstanceOf(IllegalArgumentException.class);
    }
}
