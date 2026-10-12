package io.github.marcosaalbanojunior.prontuario.domain.service.quickentry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.marcosaalbanojunior.prontuario.domain.model.entry.BodyRegion;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.Intensity;
import io.github.marcosaalbanojunior.prontuario.domain.model.quickentry.MissingInfo;
import io.github.marcosaalbanojunior.prontuario.domain.model.quickentry.ParseOutcome.NeedsMore;
import io.github.marcosaalbanojunior.prontuario.domain.model.quickentry.ParseOutcome.Parsed;
import io.github.marcosaalbanojunior.prontuario.domain.model.quickentry.ParseOutcome.Rejected;
import io.github.marcosaalbanojunior.prontuario.domain.model.quickentry.ParseOutcome;
import io.github.marcosaalbanojunior.prontuario.domain.model.quickentry.QuickCommand;
import io.github.marcosaalbanojunior.prontuario.domain.model.quickentry.QuickEntry;
import io.github.marcosaalbanojunior.prontuario.domain.model.quickentry.RejectionReason;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("QuickEntryParser")
class QuickEntryParserTest {

    private static final BodyRegionVocabulary VOCABULARY = new BodyRegionVocabulary(Map.of(
            BodyRegion.HEAD, List.of("cabeça"),
            BodyRegion.RIGHT_KNEE, List.of("joelho direito"),
            BodyRegion.LOWER_BACK, List.of("lombar", "costas baixa")));

    private final QuickEntryParser parser = new QuickEntryParser(VOCABULARY);

    @Nested
    @DisplayName("/agua")
    class Water {

        @ParameterizedTest(name = "\"{0}\" is {1}")
        @MethodSource("io.github.marcosaalbanojunior.prontuario.domain.service.quickentry.QuickEntryParserTest#water")
        @DisplayName("reads milliliters or liters, asks when empty and rejects nonsense")
        void should_parseWater_when_argumentsVary(String args, ParseOutcome expected) {
            assertThat(parser.parse(QuickCommand.WATER, args)).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("/sono")
    class Sleep {

        @ParameterizedTest(name = "\"{0}\" is {1}")
        @MethodSource("io.github.marcosaalbanojunior.prontuario.domain.service.quickentry.QuickEntryParserTest#sleep")
        @DisplayName("reads a duration or bedtime and wake-up time")
        void should_parseSleep_when_argumentsVary(String args, ParseOutcome expected) {
            assertThat(parser.parse(QuickCommand.SLEEP, args)).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("/treino")
    class Exercise {

        @ParameterizedTest(name = "\"{0}\" is {1}")
        @MethodSource("io.github.marcosaalbanojunior.prontuario.domain.service.quickentry.QuickEntryParserTest#exercise")
        @DisplayName("reads an optional activity followed by a duration")
        void should_parseExercise_when_argumentsVary(String args, ParseOutcome expected) {
            assertThat(parser.parse(QuickCommand.EXERCISE, args)).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("/peso")
    class Weight {

        @ParameterizedTest(name = "\"{0}\" is {1}")
        @MethodSource("io.github.marcosaalbanojunior.prontuario.domain.service.quickentry.QuickEntryParserTest#weight")
        @DisplayName("reads kilograms with a comma or a dot")
        void should_parseWeight_when_argumentsVary(String args, ParseOutcome expected) {
            assertThat(parser.parse(QuickCommand.WEIGHT, args)).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("/dor")
    class Pain {

        @ParameterizedTest(name = "\"{0}\" is {1}")
        @MethodSource("io.github.marcosaalbanojunior.prontuario.domain.service.quickentry.QuickEntryParserTest#pain")
        @DisplayName("reads a body region and an optional intensity before or after it")
        void should_parsePain_when_argumentsVary(String args, ParseOutcome expected) {
            assertThat(parser.parse(QuickCommand.PAIN, args)).isEqualTo(expected);
        }
    }

    @Test
    @DisplayName("refuses null arguments")
    void should_throw_when_argumentsAreNull() {
        assertThatNullPointerException().isThrownBy(() -> parser.parse(QuickCommand.WATER, null));
        assertThatNullPointerException().isThrownBy(() -> parser.parse(null, "500"));
    }

    static Stream<Arguments> water() {
        return Stream.of(
                Arguments.of("500", parsed(new QuickEntry.Water(new BigDecimal("500")))),
                Arguments.of("500ml", parsed(new QuickEntry.Water(new BigDecimal("500")))),
                Arguments.of(" 500 ML ", parsed(new QuickEntry.Water(new BigDecimal("500")))),
                Arguments.of("0,5l", parsed(new QuickEntry.Water(new BigDecimal("500")))),
                Arguments.of("1.5L", parsed(new QuickEntry.Water(new BigDecimal("1500")))),
                Arguments.of("", new NeedsMore(MissingInfo.WATER_AMOUNT)),
                Arguments.of("5", rejected(RejectionReason.OUT_OF_RANGE)),
                Arguments.of("5001", rejected(RejectionReason.OUT_OF_RANGE)),
                Arguments.of("muita", rejected(RejectionReason.NOT_UNDERSTOOD)));
    }

    static Stream<Arguments> sleep() {
        return Stream.of(
                Arguments.of("6h40", parsed(new QuickEntry.SleepDuration(hoursAndMinutes(6, 40)))),
                Arguments.of("7", parsed(new QuickEntry.SleepDuration(Duration.ofHours(7)))),
                Arguments.of("6:40", parsed(new QuickEntry.SleepDuration(hoursAndMinutes(6, 40)))),
                Arguments.of("7h", parsed(new QuickEntry.SleepDuration(Duration.ofHours(7)))),
                Arguments.of("420min", parsed(new QuickEntry.SleepDuration(Duration.ofHours(7)))),
                Arguments.of("23:30 06:10", parsed(new QuickEntry.SleepSpan(LocalTime.of(23, 30), LocalTime.of(6, 10)))),
                Arguments.of("", new NeedsMore(MissingInfo.SLEEP_DURATION)),
                Arguments.of("25h", rejected(RejectionReason.OUT_OF_RANGE)),
                Arguments.of("0", rejected(RejectionReason.OUT_OF_RANGE)),
                Arguments.of("6h75", rejected(RejectionReason.NOT_UNDERSTOOD)),
                Arguments.of("25:00 06:10", rejected(RejectionReason.NOT_UNDERSTOOD)),
                Arguments.of("23:30 06:75", rejected(RejectionReason.NOT_UNDERSTOOD)),
                Arguments.of("23:30 cedo", rejected(RejectionReason.NOT_UNDERSTOOD)),
                Arguments.of("23:30 06:10 08:00", rejected(RejectionReason.NOT_UNDERSTOOD)),
                Arguments.of("bem", rejected(RejectionReason.NOT_UNDERSTOOD)));
    }

    static Stream<Arguments> exercise() {
        return Stream.of(
                Arguments.of("musculação 50", parsed(new QuickEntry.Exercise(Optional.of("musculação"), Duration.ofMinutes(50)))),
                Arguments.of("Corrida Leve 1h30", parsed(new QuickEntry.Exercise(Optional.of("corrida leve"), hoursAndMinutes(1, 30)))),
                Arguments.of("45", parsed(new QuickEntry.Exercise(Optional.empty(), Duration.ofMinutes(45)))),
                Arguments.of("90min", parsed(new QuickEntry.Exercise(Optional.empty(), Duration.ofMinutes(90)))),
                Arguments.of("", new NeedsMore(MissingInfo.EXERCISE_DURATION)),
                Arguments.of("musculação", new NeedsMore(MissingInfo.EXERCISE_DURATION)),
                Arguments.of("13h", rejected(RejectionReason.OUT_OF_RANGE)),
                Arguments.of("0", rejected(RejectionReason.OUT_OF_RANGE)));
    }

    static Stream<Arguments> weight() {
        return Stream.of(
                Arguments.of("82,4", parsed(new QuickEntry.Weight(new BigDecimal("82.4")))),
                Arguments.of("82.4", parsed(new QuickEntry.Weight(new BigDecimal("82.4")))),
                Arguments.of("82.4 kg", parsed(new QuickEntry.Weight(new BigDecimal("82.4")))),
                Arguments.of("", new NeedsMore(MissingInfo.WEIGHT_VALUE)),
                Arguments.of("19", rejected(RejectionReason.OUT_OF_RANGE)),
                Arguments.of("401", rejected(RejectionReason.OUT_OF_RANGE)),
                Arguments.of("pesado", rejected(RejectionReason.NOT_UNDERSTOOD)));
    }

    static Stream<Arguments> pain() {
        return Stream.of(
                Arguments.of("joelho direito 4", parsed(pain(BodyRegion.RIGHT_KNEE, 4))),
                Arguments.of("Cabeca", parsed(new QuickEntry.Pain(BodyRegion.HEAD, Optional.empty()))),
                Arguments.of("4 lombar", parsed(pain(BodyRegion.LOWER_BACK, 4))),
                Arguments.of("costas  baixa 0", parsed(pain(BodyRegion.LOWER_BACK, 0))),
                Arguments.of("cabeça 10", parsed(pain(BodyRegion.HEAD, 10))),
                Arguments.of("", new NeedsMore(MissingInfo.BODY_REGION)),
                Arguments.of("4", new NeedsMore(MissingInfo.BODY_REGION)),
                Arguments.of("cabeça 11", rejected(RejectionReason.INTENSITY_OUT_OF_RANGE)),
                Arguments.of("cabeça 1000", rejected(RejectionReason.UNKNOWN_BODY_REGION)),
                Arguments.of("orelha 3", rejected(RejectionReason.UNKNOWN_BODY_REGION)));
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

    private static Duration hoursAndMinutes(int hours, int minutes) {
        return Duration.ofHours(hours).plusMinutes(minutes);
    }
}
