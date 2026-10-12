package io.github.marcosaalbanojunior.prontuario.entries.domain.quick;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Turns the arguments of a quick command into a {@link QuickEntry}, a question, or a rejection. */
public final class QuickEntryParser {

    private final Map<QuickCommand, ArgsParser> parsers = new EnumMap<>(QuickCommand.class);

    public QuickEntryParser(BodyRegionVocabulary vocabulary) {
        parsers.put(QuickCommand.WATER, new WaterArgs());
        parsers.put(QuickCommand.SLEEP, new SleepArgs());
        parsers.put(QuickCommand.EXERCISE, new ExerciseArgs());
        parsers.put(QuickCommand.WEIGHT, new WeightArgs());
        parsers.put(QuickCommand.PAIN, new PainArgs(vocabulary));
    }

    public ParseOutcome parse(QuickCommand command, String args) {
        Objects.requireNonNull(command);
        Objects.requireNonNull(args);
        return parsers.get(command).parse(args);
    }
}
