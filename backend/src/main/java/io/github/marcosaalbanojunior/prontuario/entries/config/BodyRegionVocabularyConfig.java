package io.github.marcosaalbanojunior.prontuario.entries.config;

import io.github.marcosaalbanojunior.prontuario.entries.domain.BodyRegion;
import io.github.marcosaalbanojunior.prontuario.entries.domain.quick.BodyRegionVocabulary;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.io.ClassPathResource;

@Configuration(proxyBeanMethods = false)
@ImportRuntimeHints(BodyRegionVocabularyConfig.VocabularyHints.class)
class BodyRegionVocabularyConfig {

    static final String RESOURCE = "entries/body-regions_pt_BR.properties";

    @Bean
    BodyRegionVocabulary bodyRegionVocabulary() {
        return new BodyRegionVocabulary(aliasesByRegion(load()));
    }

    private static Properties load() {
        var properties = new Properties();
        try (var reader = new InputStreamReader(new ClassPathResource(RESOURCE).getInputStream(), StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + RESOURCE, e);
        }
        return properties;
    }

    private static Map<BodyRegion, List<String>> aliasesByRegion(Properties properties) {
        var aliases = new EnumMap<BodyRegion, List<String>>(BodyRegion.class);
        properties.forEach((region, words) -> aliases.put(
                BodyRegion.valueOf(region.toString().toUpperCase(Locale.ROOT)),
                Arrays.stream(words.toString().split(",")).map(String::strip).filter(word -> !word.isEmpty()).toList()));
        return aliases;
    }

    static class VocabularyHints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            hints.resources().registerPattern(RESOURCE);
        }
    }
}
