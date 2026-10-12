package io.github.marcosaalbanojunior.prontuario.domain.service.quickentry;

import io.github.marcosaalbanojunior.prontuario.domain.model.entry.BodyRegion;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Words the user types for each body region, matched without accents or case. */
public final class BodyRegionVocabulary {

    private final Map<String, BodyRegion> regionsByAlias = new HashMap<>();

    public BodyRegionVocabulary(Map<BodyRegion, List<String>> aliasesByRegion) {
        aliasesByRegion.forEach((region, aliases) -> aliases.forEach(alias -> register(Text.normalize(alias), region)));
    }

    public Optional<BodyRegion> find(String text) {
        return Optional.ofNullable(regionsByAlias.get(Text.normalize(text)));
    }

    private void register(String alias, BodyRegion region) {
        var previous = regionsByAlias.putIfAbsent(alias, region);
        if (previous != null && previous != region) {
            throw new IllegalArgumentException("alias '%s' maps to both %s and %s".formatted(alias, previous, region));
        }
    }
}
