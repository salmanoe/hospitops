package id.co.hospitops.shared;

/**
 * Strategy pattern for tax calculation.
 * Eliminates the TAX_RATE magic constant scattered across billing domain.
 *
 * <p>Adding a named rate: {@code TaxPolicy.Standard.PPN_12(12)} — one line, zero new methods.
 * <p>Creating a rate from a stored percentage: {@code TaxPolicy.of(11)} — used by billing
 * when reading the rate from {@code hotel_policy_config} at runtime.
 */
public interface TaxPolicy {

    Money calculate(Money subtotal);

    /**
     * Creates an anonymous {@link TaxPolicy} that applies {@code percent}% to the subtotal.
     * Use this when the rate comes from a runtime value (e.g. hotel policy config)
     * rather than a compile-time constant.
     *
     * @param percent tax rate, 0–100 inclusive
     * @throws IllegalArgumentException if percent is outside 0–100
     */
    static TaxPolicy of(int percent) {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("Tax percent must be between 0 and 100, got: " + percent);
        }
        return subtotal -> subtotal.percentage(percent);
    }

    enum Standard implements TaxPolicy {
        PPN_11(11);

        private final int taxPercent;

        Standard(int taxPercent) {
            this.taxPercent = taxPercent;
        }

        @Override
        public Money calculate(Money subtotal) {
            return subtotal.percentage(taxPercent);
        }

        public String description() {
            return "PPN " + taxPercent + "%";
        }

        public int getPercent() {
            return taxPercent;
        }
    }
}
