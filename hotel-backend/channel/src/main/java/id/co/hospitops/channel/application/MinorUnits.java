package id.co.hospitops.channel.application;

import id.co.hospitops.shared.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

/**
 * Converts amounts to integer minor units (the form Channex expects on ARI
 * writes). The factor is currency-dependent: IDR has 0 fraction digits so the
 * minor unit equals the major unit; USD has 2, so 120.00 → 12000.
 */
public final class MinorUnits {

    private MinorUnits() {
    }

    public static long of(Money money) {
        return of(money.amount(), money.currency());
    }

    public static long of(BigDecimal majorAmount, Currency currency) {
        return majorAmount
                .movePointRight(currency.getDefaultFractionDigits())
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }
}
