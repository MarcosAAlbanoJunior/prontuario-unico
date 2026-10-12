package io.github.marcosaalbanojunior.prontuario.application.usecase.telegram;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Brazilian formatting: decimal comma, dd/MM dates, 24-hour clock, in the user's time zone. */
public final class PtBrFormat {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final Clock clock;
    private final ZoneId zone;

    public PtBrFormat(Clock clock, ZoneId zone) {
        this.clock = clock;
        this.zone = zone;
    }

    String number(BigDecimal value) {
        var format = NumberFormat.getNumberInstance(PT_BR);
        format.setMaximumFractionDigits(1);
        return format.format(value);
    }

    /** "500 mL", "6 h 40 min", "82,4 kg": the unit codes are the stored ones. */
    String quantity(BigDecimal value, String unit) {
        return switch (unit) {
            case "ml" -> number(value) + " mL";
            case "min" -> minutes(value.longValue());
            default -> number(value) + " " + unit;
        };
    }

    /** Time only for today; date and time otherwise. */
    String time(Instant instant) {
        var local = instant.atZone(zone);
        var today = LocalDate.now(clock.withZone(zone));
        return local.toLocalDate().equals(today) ? TIME.format(local) : DATE_TIME.format(local);
    }

    private static String minutes(long total) {
        long hours = total / 60;
        long minutes = total % 60;
        if (hours == 0) {
            return minutes + " min";
        }
        return minutes == 0 ? hours + " h" : hours + " h " + minutes + " min";
    }
}
