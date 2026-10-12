package io.github.marcosaalbanojunior.prontuario.domain.service.quickentry;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Pattern;

/** Decimal numbers typed in pt-BR or en style: "82,4" and "82.4" are the same value. */
final class Numbers {

    private static final Pattern DECIMAL = Pattern.compile("\\d+([.,]\\d+)?");
    private static final Pattern INTEGER = Pattern.compile("\\d{1,3}");

    private Numbers() {}

    static Optional<BigDecimal> decimal(String text) {
        if (!DECIMAL.matcher(text).matches()) {
            return Optional.empty();
        }
        return Optional.of(new BigDecimal(text.replace(',', '.')));
    }

    static Optional<Integer> smallInteger(String text) {
        return INTEGER.matcher(text).matches() ? Optional.of(Integer.parseInt(text)) : Optional.empty();
    }

    static boolean within(BigDecimal value, int min, int max) {
        return value.compareTo(BigDecimal.valueOf(min)) >= 0 && value.compareTo(BigDecimal.valueOf(max)) <= 0;
    }
}
