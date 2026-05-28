package id.co.hospitops.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

public record Money(BigDecimal amount, Currency currency) {

    public static final Currency IDR = Currency.getInstance("IDR");
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public static Money of(long amount) {
        return new Money(BigDecimal.valueOf(amount), IDR);
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount, IDR);
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO, IDR);
    }

    /**
     * Adds {@code other} to this amount.
     *
     * @throws IllegalArgumentException if the currencies differ
     */
    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    /**
     * Subtracts {@code other} from this amount.
     *
     * @throws IllegalArgumentException if the currencies differ
     */
    public Money subtract(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    private void requireSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Currency mismatch: cannot operate on " + this.currency +
                    " and " + other.currency);
        }
    }

    public Money multiply(int factor) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(factor)), this.currency);
    }

    /**
     * Returns {@code percent}% of this amount, rounded half-up to 2 decimal places.
     * Used by {@link id.co.hospitops.shared.TaxPolicy} to compute tax amounts.
     */
    public Money percentage(int percent) {
        return new Money(
                amount.multiply(BigDecimal.valueOf(percent))
                      .divide(HUNDRED, 2, RoundingMode.HALF_UP),
                currency);
    }

    public boolean isGreaterThan(Money other) {
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }
}
