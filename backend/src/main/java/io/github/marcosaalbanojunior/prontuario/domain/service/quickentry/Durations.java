package io.github.marcosaalbanojunior.prontuario.domain.service.quickentry;

import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Durations ("50", "1h30", "6h40", "90min", "6:40") and clock times ("23:30"). */
final class Durations {

    private static final Pattern PLAIN_NUMBER = Pattern.compile("(\\d{1,4})");
    private static final Pattern HOURS_AND_MINUTES = Pattern.compile("(\\d{1,2})h(\\d{1,2})?(?:min|m)?");
    private static final Pattern MINUTES = Pattern.compile("(\\d{1,4})(?:min|m)");
    private static final Pattern CLOCK = Pattern.compile("(\\d{1,2}):(\\d{2})");

    private Durations() {}

    /** A bare number is read in {@code bareUnit}: minutes for exercise, hours for sleep. */
    static Optional<Duration> parse(String token, ChronoUnit bareUnit) {
        Matcher matcher;
        if ((matcher = PLAIN_NUMBER.matcher(token)).matches()) {
            return Optional.of(Duration.of(Long.parseLong(matcher.group(1)), bareUnit));
        }
        if ((matcher = MINUTES.matcher(token)).matches()) {
            return Optional.of(Duration.ofMinutes(Long.parseLong(matcher.group(1))));
        }
        if ((matcher = HOURS_AND_MINUTES.matcher(token)).matches() || (matcher = CLOCK.matcher(token)).matches()) {
            return hoursAndMinutes(matcher.group(1), matcher.group(2));
        }
        return Optional.empty();
    }

    static Optional<LocalTime> clockTime(String token) {
        var matcher = CLOCK.matcher(token);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        int hour = Integer.parseInt(matcher.group(1));
        int minute = Integer.parseInt(matcher.group(2));
        return hour < 24 && minute < 60 ? Optional.of(LocalTime.of(hour, minute)) : Optional.empty();
    }

    private static Optional<Duration> hoursAndMinutes(String hours, String minutes) {
        int minutePart = minutes == null ? 0 : Integer.parseInt(minutes);
        if (minutePart >= 60) {
            return Optional.empty();
        }
        return Optional.of(Duration.ofHours(Long.parseLong(hours)).plusMinutes(minutePart));
    }
}
