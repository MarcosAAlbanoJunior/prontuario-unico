package io.github.marcosaalbanojunior.prontuario.domain.model.goal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("GoalPeriod and PeriodWindow")
class GoalPeriodTest {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    @Nested
    @DisplayName("windowContaining(...)")
    class WindowContaining {

        @ParameterizedTest(name = "{0} containing {1} is [{2}, {3})")
        @CsvSource({
            "DAY,  2026-10-12, 2026-10-12T03:00:00Z, 2026-10-13T03:00:00Z",
            "WEEK, 2026-10-12, 2026-10-12T03:00:00Z, 2026-10-19T03:00:00Z",
            "WEEK, 2026-10-14, 2026-10-12T03:00:00Z, 2026-10-19T03:00:00Z",
            "WEEK, 2026-10-18, 2026-10-12T03:00:00Z, 2026-10-19T03:00:00Z",
            "WEEK, 2026-10-19, 2026-10-19T03:00:00Z, 2026-10-26T03:00:00Z",
            "WEEK, 2026-12-31, 2026-12-28T03:00:00Z, 2027-01-04T03:00:00Z",
        })
        @DisplayName("uses local midnights and weeks that start on Monday")
        void should_useLocalMidnightsAndMondays_when_computingWindow(GoalPeriod period, LocalDate today, Instant from, Instant to) {
            assertThat(period.windowContaining(today, SAO_PAULO)).isEqualTo(new PeriodWindow(from, to));
        }
    }

    @Nested
    @DisplayName("PeriodWindow")
    class Window {

        @Test
        @DisplayName("rejects a window that does not start before it ends")
        void should_reject_when_fromIsNotBeforeTo() {
            var instant = Instant.parse("2026-10-12T03:00:00Z");

            assertThatThrownBy(() -> new PeriodWindow(instant, instant)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
