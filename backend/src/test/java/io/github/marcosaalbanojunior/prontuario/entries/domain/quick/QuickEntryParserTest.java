package io.github.marcosaalbanojunior.prontuario.entries.domain.quick;

import static io.github.marcosaalbanojunior.prontuario.entries.domain.quick.QuickCommand.EXERCISE;
import static io.github.marcosaalbanojunior.prontuario.entries.domain.quick.QuickCommand.PAIN;
import static io.github.marcosaalbanojunior.prontuario.entries.domain.quick.QuickCommand.SLEEP;
import static io.github.marcosaalbanojunior.prontuario.entries.domain.quick.QuickCommand.WATER;
import static io.github.marcosaalbanojunior.prontuario.entries.domain.quick.QuickCommand.WEIGHT;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.marcosaalbanojunior.prontuario.entries.domain.BodyRegion;
import io.github.marcosaalbanojunior.prontuario.entries.domain.Intensity;
import io.github.marcosaalbanojunior.prontuario.entries.domain.quick.ParseOutcome.NeedsMore;
import io.github.marcosaalbanojunior.prontuario.entries.domain.quick.ParseOutcome.Parsed;
import io.github.marcosaalbanojunior.prontuario.entries.domain.quick.ParseOutcome.Rejected;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class QuickEntryParserTest {

    private static final BodyRegionVocabulary VOCABULARY = new BodyRegionVocabulary(Map.of(
            BodyRegion.HEAD, List.of("cabeça"),
            BodyRegion.RIGHT_KNEE, List.of("joelho direito"),
            BodyRegion.LOWER_BACK, List.of("lombar", "costas baixa")));

    private final QuickEntryParser parser = new QuickEntryParser(VOCABULARY);

    @ParameterizedTest(name = "/{0} \"{1}\" -> {2}")
    @MethodSource({"water", "sleep", "exercise", "weight", "pain"})
    void shouldParseQuickCommandArguments(QuickCommand command, String args, ParseOutcome expected) {
        assertThat(parser.parse(command, args)).isEqualTo(expected);
    }

    static Stream<Arguments> water() {
        return Stream.of(
                Arguments.of(WATER, "500", parsed(new QuickEntry.Water(ml("500")))),
                Arguments.of(WATER, "500ml", parsed(new QuickEntry.Water(ml("500")))),
                Arguments.of(WATER, " 500 ML ", parsed(new QuickEntry.Water(ml("500")))),
                Arguments.of(WATER, "0,5l", parsed(new QuickEntry.Water(ml("500")))),
                Arguments.of(WATER, "1.5L", parsed(new QuickEntry.Water(ml("1500")))),
                Arguments.of(WATER, "", new NeedsMore(MissingInfo.WATER_AMOUNT)),
                Arguments.of(WATER, "5", rejected(RejectionReason.OUT_OF_RANGE)),
                Arguments.of(WATER, "5001", rejected(RejectionReason.OUT_OF_RANGE)),
                Arguments.of(WATER, "muita", rejected(RejectionReason.NOT_UNDERSTOOD)));
    }

    static Stream<Arguments> sleep() {
        return Stream.of(
                Arguments.of(SLEEP, "6h40", parsed(new QuickEntry.SleepDuration(hoursAndMinutes(6, 40)))),
                Arguments.of(SLEEP, "7", parsed(new QuickEntry.SleepDuration(Duration.ofHours(7)))),
                Arguments.of(SLEEP, "6:40", parsed(new QuickEntry.SleepDuration(hoursAndMinutes(6, 40)))),
                Arguments.of(SLEEP, "7h", parsed(new QuickEntry.SleepDuration(Duration.ofHours(7)))),
                Arguments.of(SLEEP, "23:30 06:10", parsed(new QuickEntry.SleepSpan(LocalTime.of(23, 30), LocalTime.of(6, 10)))),
                Arguments.of(SLEEP, "", new NeedsMore(MissingInfo.SLEEP_DURATION)),
                Arguments.of(SLEEP, "25h", rejected(RejectionReason.OUT_OF_RANGE)),
                Arguments.of(SLEEP, "0", rejected(RejectionReason.OUT_OF_RANGE)),
                Arguments.of(SLEEP, "6h75", rejected(RejectionReason.NOT_UNDERSTOOD)),
                Arguments.of(SLEEP, "25:00 06:10", rejected(RejectionReason.NOT_UNDERSTOOD)),
                Arguments.of(SLEEP, "bem", rejected(RejectionReason.NOT_UNDERSTOOD)));
    }

    static Stream<Arguments> exercise() {
        return Stream.of(
                Arguments.of(EXERCISE, "musculação 50", parsed(new QuickEntry.Exercise(Optional.of("musculação"), Duration.ofMinutes(50)))),
                Arguments.of(EXERCISE, "Corrida Leve 1h30", parsed(new QuickEntry.Exercise(Optional.of("corrida leve"), hoursAndMinutes(1, 30)))),
                Arguments.of(EXERCISE, "45", parsed(new QuickEntry.Exercise(Optional.empty(), Duration.ofMinutes(45)))),
                Arguments.of(EXERCISE, "90min", parsed(new QuickEntry.Exercise(Optional.empty(), Duration.ofMinutes(90)))),
                Arguments.of(EXERCISE, "", new NeedsMore(MissingInfo.EXERCISE_DURATION)),
                Arguments.of(EXERCISE, "musculação", new NeedsMore(MissingInfo.EXERCISE_DURATION)),
                Arguments.of(EXERCISE, "13h", rejected(RejectionReason.OUT_OF_RANGE)),
                Arguments.of(EXERCISE, "0", rejected(RejectionReason.OUT_OF_RANGE)));
    }

    static Stream<Arguments> weight() {
        return Stream.of(
                Arguments.of(WEIGHT, "82,4", parsed(new QuickEntry.Weight(new BigDecimal("82.4")))),
                Arguments.of(WEIGHT, "82.4", parsed(new QuickEntry.Weight(new BigDecimal("82.4")))),
                Arguments.of(WEIGHT, "82.4 kg", parsed(new QuickEntry.Weight(new BigDecimal("82.4")))),
                Arguments.of(WEIGHT, "", new NeedsMore(MissingInfo.WEIGHT_VALUE)),
                Arguments.of(WEIGHT, "19", rejected(RejectionReason.OUT_OF_RANGE)),
                Arguments.of(WEIGHT, "401", rejected(RejectionReason.OUT_OF_RANGE)),
                Arguments.of(WEIGHT, "pesado", rejected(RejectionReason.NOT_UNDERSTOOD)));
    }

    static Stream<Arguments> pain() {
        return Stream.of(
                Arguments.of(PAIN, "joelho direito 4", parsed(pain(BodyRegion.RIGHT_KNEE, 4))),
                Arguments.of(PAIN, "Cabeca", parsed(new QuickEntry.Pain(BodyRegion.HEAD, Optional.empty()))),
                Arguments.of(PAIN, "4 lombar", parsed(pain(BodyRegion.LOWER_BACK, 4))),
                Arguments.of(PAIN, "costas  baixa 0", parsed(pain(BodyRegion.LOWER_BACK, 0))),
                Arguments.of(PAIN, "cabeça 10", parsed(pain(BodyRegion.HEAD, 10))),
                Arguments.of(PAIN, "", new NeedsMore(MissingInfo.BODY_REGION)),
                Arguments.of(PAIN, "4", new NeedsMore(MissingInfo.BODY_REGION)),
                Arguments.of(PAIN, "cabeça 11", rejected(RejectionReason.INTENSITY_OUT_OF_RANGE)),
                Arguments.of(PAIN, "orelha 3", rejected(RejectionReason.UNKNOWN_BODY_REGION)));
    }

    private static Parsed parsed(QuickEntry entry) {
        return new Parsed(entry);
    }

    private static Rejected rejected(RejectionReason reason) {
        return new Rejected(reason);
    }

    private static QuickEntry.Pain pain(BodyRegion region, int intensity) {
        return new QuickEntry.Pain(region, Optional.of(new Intensity(intensity)));
    }

    private static BigDecimal ml(String value) {
        return new BigDecimal(value);
    }

    private static Duration hoursAndMinutes(int hours, int minutes) {
        return Duration.ofHours(hours).plusMinutes(minutes);
    }
}
