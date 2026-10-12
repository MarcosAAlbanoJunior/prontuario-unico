package io.github.marcosaalbanojunior.prontuario.application.usecase.telegram;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.marcosaalbanojunior.prontuario.domain.model.entry.BodyRegion;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.EventDetails;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.EventType;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.Intensity;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.MeasureUnit;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.Measurement;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.TimePrecision;
import io.github.marcosaalbanojunior.prontuario.support.PtBrMessages;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("EntryLines.describe(...)")
class EntryLinesTest {

    private static final Instant START = Instant.parse("2026-10-12T09:10:00Z");
    private static final Instant END = Instant.parse("2026-10-12T17:32:00Z");

    private final EntryLines lines = new EntryLines(PtBrMessages.load(),
            new PtBrFormat(Clock.fixed(END, ZoneId.of("UTC")), ZoneId.of("America/Sao_Paulo")));

    @ParameterizedTest(name = "{0}")
    @MethodSource("events")
    @DisplayName("describes every kind of event in one pt-BR line")
    void should_describeInPortuguese_when_eventIsGiven(String expected, EventDetails details) {
        assertThat(lines.describe(Events.event(details))).isEqualTo(expected);
    }

    static Stream<Arguments> events() {
        return Stream.of(
                Arguments.of("Água: 500 mL às 06:10", details(EventType.WATER, quantity(500, MeasureUnit.MILLILITER))),
                Arguments.of("Peso: 82,4 kg às 06:10", details(EventType.WEIGHT, quantity(82.4, MeasureUnit.KILOGRAM))),
                Arguments.of("Sono: 8 h 22 min, das 06:10 às 14:32", interval(EventType.SLEEP, Optional.empty())),
                Arguments.of("Treino: 8 h 22 min, até 14:32", interval(EventType.EXERCISE, Optional.empty())),
                Arguments.of("Treino de musculação: 8 h 22 min, até 14:32", interval(EventType.EXERCISE, Optional.of("musculação"))),
                Arguments.of("Dor: joelho direito, intensidade 4, às 06:10", pain(Optional.of(new Intensity(4)))),
                Arguments.of("Dor: joelho direito, às 06:10", pain(Optional.empty())),
                Arguments.of("Dor: , às 06:10", new EventDetails(EventType.SYMPTOM, Optional.of("dor"), Optional.empty(),
                        Optional.empty(), Optional.empty(), START, Optional.empty(), TimePrecision.EXACT)),
                Arguments.of("Registro às 06:10", details(EventType.MEAL, Optional.empty())),
                Arguments.of("Registro às 06:10", details(EventType.MEDICATION, Optional.empty())),
                Arguments.of("Registro às 06:10", details(EventType.NOTE, Optional.empty())));
    }

    private static Optional<Measurement> quantity(double value, MeasureUnit unit) {
        return Optional.of(new Measurement(BigDecimal.valueOf(value), unit));
    }

    private static EventDetails details(EventType type, Optional<Measurement> measurement) {
        return new EventDetails(type, Optional.empty(), Optional.empty(), measurement, Optional.empty(), START,
                Optional.empty(), TimePrecision.EXACT);
    }

    private static EventDetails interval(EventType type, Optional<String> concept) {
        return new EventDetails(type, concept, Optional.empty(), quantity(502, MeasureUnit.MINUTE), Optional.empty(),
                START, Optional.of(END), TimePrecision.APPROX);
    }

    private static EventDetails pain(Optional<Intensity> intensity) {
        return new EventDetails(EventType.SYMPTOM, Optional.of("dor"), Optional.of(BodyRegion.RIGHT_KNEE), Optional.empty(),
                intensity, START, Optional.empty(), TimePrecision.EXACT);
    }
}
