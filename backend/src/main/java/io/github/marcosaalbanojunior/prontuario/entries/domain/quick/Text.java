package io.github.marcosaalbanojunior.prontuario.entries.domain.quick;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Normalization shared by the parsers: users type with or without accents and in any case. */
final class Text {

    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private Text() {}

    static String normalize(String text) {
        var withoutAccents = COMBINING_MARKS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("");
        return clean(withoutAccents);
    }

    static String clean(String text) {
        return WHITESPACE.matcher(text.strip().toLowerCase(Locale.ROOT)).replaceAll(" ");
    }

    static List<String> tokens(String text) {
        var cleaned = clean(text);
        return cleaned.isEmpty() ? List.of() : Arrays.asList(cleaned.split(" "));
    }
}
