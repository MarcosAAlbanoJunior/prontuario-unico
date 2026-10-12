package io.github.marcosaalbanojunior.prontuario.entries.domain.quick;

import static io.github.marcosaalbanojunior.prontuario.entries.domain.quick.ParseOutcome.NeedsMore;
import static io.github.marcosaalbanojunior.prontuario.entries.domain.quick.ParseOutcome.Parsed;
import static io.github.marcosaalbanojunior.prontuario.entries.domain.quick.ParseOutcome.Rejected;

import io.github.marcosaalbanojunior.prontuario.entries.domain.Intensity;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A body region and an optional 0–10 intensity: "joelho direito 4", "cabeça".
 * The intensity may come first, because an answer to "Onde é a dor?" is appended after it.
 */
final class PainArgs implements ArgsParser {

    private final BodyRegionVocabulary vocabulary;

    PainArgs(BodyRegionVocabulary vocabulary) {
        this.vocabulary = vocabulary;
    }

    @Override
    public ParseOutcome parse(String args) {
        var tokens = new ArrayList<>(Text.tokens(Text.normalize(args)));
        var intensity = takeIntensity(tokens);
        if (intensity.filter(value -> value > Intensity.MAX).isPresent()) {
            return new Rejected(RejectionReason.INTENSITY_OUT_OF_RANGE);
        }
        if (tokens.isEmpty()) {
            return new NeedsMore(MissingInfo.BODY_REGION);
        }
        return vocabulary.find(String.join(" ", tokens))
                .<ParseOutcome>map(region -> new Parsed(new QuickEntry.Pain(region, intensity.map(Intensity::new))))
                .orElse(new Rejected(RejectionReason.UNKNOWN_BODY_REGION));
    }

    private static Optional<Integer> takeIntensity(List<String> tokens) {
        if (tokens.isEmpty()) {
            return Optional.empty();
        }
        var last = Numbers.smallInteger(tokens.getLast());
        if (last.isPresent()) {
            tokens.removeLast();
            return last;
        }
        var first = Numbers.smallInteger(tokens.getFirst());
        first.ifPresent(value -> tokens.removeFirst());
        return first;
    }
}
