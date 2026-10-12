package io.github.marcosaalbanojunior.prontuario.telegram.adapter.out.chat;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PtBrFormatTest {

    private final PtBrFormat format = new PtBrFormat(
            Clock.fixed(Instant.parse("2026-10-12T17:00:00Z"), ZoneId.of("UTC")), ZoneId.of("America/Sao_Paulo"));

    @ParameterizedTest(name = "{0} {1} -> {2}")
    @CsvSource(delimiter = '|', value = {
        "500   | ml  | 500 mL",
        "1500  | ml  | 1.500 mL",
        "82.4  | kg  | 82,4 kg",
        "50    | min | 50 min",
        "420   | min | 7 h",
        "400   | min | 6 h 40 min",
    })
    void shouldFormatQuantitiesTheBrazilianWay(BigDecimal value, String unit, String expected) {
        assertThat(format.quantity(value, unit)).isEqualTo(expected);
    }

    @Test
    void shouldShowOnlyTheTimeForToday() {
        assertThat(format.time(Instant.parse("2026-10-12T17:32:00Z"))).isEqualTo("14:32");
    }

    @Test
    void shouldShowTheDateForOtherDays() {
        assertThat(format.time(Instant.parse("2026-10-12T02:30:00Z"))).isEqualTo("11/10 23:30");
    }
}
