package io.github.marcosaalbanojunior.prontuario.entries.domain.quick;

import static io.github.marcosaalbanojunior.prontuario.entries.domain.quick.ParseOutcome.NeedsMore;
import static io.github.marcosaalbanojunior.prontuario.entries.domain.quick.ParseOutcome.Parsed;
import static io.github.marcosaalbanojunior.prontuario.entries.domain.quick.ParseOutcome.Rejected;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

/** "500", "500ml", "500 ml", "0,5l", "1.5L"; a bare number is milliliters. */
final class WaterArgs implements ArgsParser {

    static final int MIN_MILLILITERS = 10;
    static final int MAX_MILLILITERS = 5000;

    private static final Pattern AMOUNT = Pattern.compile("(\\d+(?:[.,]\\d+)?)(ml|l)?");
    private static final BigDecimal MILLILITERS_PER_LITER = BigDecimal.valueOf(1000);

    @Override
    public ParseOutcome parse(String args) {
        var compact = Text.clean(args).replace(" ", "");
        if (compact.isEmpty()) {
            return new NeedsMore(MissingInfo.WATER_AMOUNT);
        }
        var matcher = AMOUNT.matcher(compact);
        if (!matcher.matches()) {
            return new Rejected(RejectionReason.NOT_UNDERSTOOD);
        }
        var value = new BigDecimal(matcher.group(1).replace(',', '.'));
        var milliliters = ("l".equals(matcher.group(2)) ? value.multiply(MILLILITERS_PER_LITER) : value)
                .setScale(0, RoundingMode.HALF_UP);
        if (!Numbers.within(milliliters, MIN_MILLILITERS, MAX_MILLILITERS)) {
            return new Rejected(RejectionReason.OUT_OF_RANGE);
        }
        return new Parsed(new QuickEntry.Water(milliliters));
    }
}
