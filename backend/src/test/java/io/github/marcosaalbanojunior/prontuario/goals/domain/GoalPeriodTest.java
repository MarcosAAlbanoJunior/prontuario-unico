package io.github.marcosaalbanojunior.prontuario.goals.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GoalPeriodTest {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    @ParameterizedTest(name = "{0} on {1} -> [{2}, {3})")
    @CsvSource({
        "DAY,  2026-10-12, 2026-10-12T03:00:00Z, 2026-10-13T03:00:00Z",
        "WEEK, 2026-10-12, 2026-10-12T03:00:00Z, 2026-10-19T03:00:00Z",
        "WEEK, 2026-10-14, 2026-10-12T03:00:00Z, 2026-10-19T03:00:00Z",
        "WEEK, 2026-10-18, 2026-10-12T03:00:00Z, 2026-10-19T03:00:00Z",
        "WEEK, 2026-10-19, 2026-10-19T03:00:00Z, 2026-10-26T03:00:00Z",
        "WEEK, 2026-12-31, 2026-12-28T03:00:00Z, 2027-01-04T03:00:00Z",
    })
    void shouldUseLocalMidnightsAndWeeksStartingOnMonday(GoalPeriod period, LocalDate today, Instant from, Instant to) {
        assertThat(period.windowContaining(today, SAO_PAULO)).isEqualTo(new PeriodWindow(from, to));
    }
}
