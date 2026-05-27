package id.co.hospitops.shared;

import org.junit.jupiter.api.*;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("TaxPolicy")
class TaxPolicyTest {

    @Nested
    @DisplayName("TaxPolicy.Standard.PPN_11")
    class Ppn11 {

        private final TaxPolicy.Standard ppn11 = TaxPolicy.Standard.PPN_11;

        @Test
        @DisplayName("calculates 11% of a standard subtotal")
        void calculatesElevenPercent() {
            Money tax = ppn11.calculate(Money.of(1_500_000L));
            assertThat(tax.amount()).isEqualByComparingTo(BigDecimal.valueOf(165_000));
        }

        @Test
        @DisplayName("returns zero tax on a zero subtotal")
        void zeroTaxOnZeroSubtotal() {
            Money tax = ppn11.calculate(Money.zero());
            assertThat(tax.isZero()).isTrue();
        }

        @Test
        @DisplayName("tax on a single-night 500,000 rate is 55,000")
        void singleNightTax() {
            Money tax = ppn11.calculate(Money.of(500_000L));
            assertThat(tax.amount()).isEqualByComparingTo(BigDecimal.valueOf(55_000));
        }

        @Test
        @DisplayName("description() includes the tax percentage")
        void descriptionIncludesTaxRate() {
            assertThat(ppn11.description()).contains("11");
        }

        @Test
        @DisplayName("getPercent() returns 11")
        void getPercentReturnsEleven() {
            assertThat(ppn11.getPercent()).isEqualTo(11);
        }

        @Test
        @DisplayName("result currency is IDR")
        void resultIsIdr() {
            Money tax = ppn11.calculate(Money.of(500_000L));
            assertThat(tax.currency()).isEqualTo(Money.IDR);
        }

        @Test
        @DisplayName("total = subtotal + tax equals subtotal * 1.11")
        void totalEqualsSubtotalPlusTax() {
            Money subtotal = Money.of(1_000_000L);
            Money tax      = ppn11.calculate(subtotal);
            Money total    = subtotal.add(tax);
            assertThat(total.amount()).isEqualByComparingTo(BigDecimal.valueOf(1_110_000));
        }
    }
}
