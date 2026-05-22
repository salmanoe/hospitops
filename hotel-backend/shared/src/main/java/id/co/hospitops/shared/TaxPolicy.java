package id.co.hospitops.shared;

/**
 * Strategy pattern for tax calculation.
 * Eliminates the TAX_RATE magic constant scattered across billing domain.
 *
 * Adding a new rate: TaxPolicy.Standard.PPN_12(12) — one line, zero new methods.
 */
public interface TaxPolicy {

    Money calculate(Money subtotal);

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
