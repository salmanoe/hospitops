package id.co.hospitops.shared;

import org.junit.jupiter.api.*;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Money")
class MoneyTest {

    // ── factories ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("of() / zero()")
    class Factories {

        @Test
        @DisplayName("of(long) wraps amount in IDR")
        void ofLongUsesIdr() {
            Money m = Money.of(500_000L);
            assertThat(m.amount()).isEqualByComparingTo(BigDecimal.valueOf(500_000));
            assertThat(m.currency()).isEqualTo(Money.IDR);
        }

        @Test
        @DisplayName("of(BigDecimal) wraps amount in IDR")
        void ofBigDecimalUsesIdr() {
            Money m = Money.of(BigDecimal.valueOf(123_456.78));
            assertThat(m.amount()).isEqualByComparingTo(BigDecimal.valueOf(123_456.78));
            assertThat(m.currency()).isEqualTo(Money.IDR);
        }

        @Test
        @DisplayName("zero() has amount = 0")
        void zeroHasAmountZero() {
            assertThat(Money.zero().amount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("zero() uses IDR currency")
        void zeroUsesIdr() {
            assertThat(Money.zero().currency()).isEqualTo(Money.IDR);
        }
    }

    // ── add ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("add()")
    class Add {

        @Test
        @DisplayName("sums two amounts")
        void sumsTwoAmounts() {
            assertThat(Money.of(100_000L).add(Money.of(200_000L)).amount())
                    .isEqualByComparingTo(BigDecimal.valueOf(300_000));
        }

        @Test
        @DisplayName("adding zero returns same amount")
        void addingZeroReturnsSameAmount() {
            Money m = Money.of(500_000L);
            assertThat(m.add(Money.zero()).amount())
                    .isEqualByComparingTo(m.amount());
        }
    }

    // ── subtract ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("subtract()")
    class Subtract {

        @Test
        @DisplayName("subtracts the second from the first")
        void subtractsAmounts() {
            assertThat(Money.of(500_000L).subtract(Money.of(200_000L)).amount())
                    .isEqualByComparingTo(BigDecimal.valueOf(300_000));
        }

        @Test
        @DisplayName("subtracting zero returns same amount")
        void subtractingZeroReturnsSameAmount() {
            Money m = Money.of(500_000L);
            assertThat(m.subtract(Money.zero()).amount())
                    .isEqualByComparingTo(m.amount());
        }

        @Test
        @DisplayName("result can be negative")
        void resultCanBeNegative() {
            assertThat(Money.of(100_000L).subtract(Money.of(200_000L)).amount())
                    .isEqualByComparingTo(BigDecimal.valueOf(-100_000));
        }
    }

    // ── multiply ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("multiply()")
    class Multiply {

        @Test
        @DisplayName("multiplies by a positive integer")
        void multipliesByPositiveInt() {
            assertThat(Money.of(500_000L).multiply(3).amount())
                    .isEqualByComparingTo(BigDecimal.valueOf(1_500_000));
        }

        @Test
        @DisplayName("multiply by 1 returns same amount")
        void multiplyByOneReturnsSame() {
            assertThat(Money.of(500_000L).multiply(1).amount())
                    .isEqualByComparingTo(BigDecimal.valueOf(500_000));
        }

        @Test
        @DisplayName("multiply by 0 returns zero")
        void multiplyByZeroReturnsZero() {
            assertThat(Money.of(500_000L).multiply(0).amount())
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // ── percentage ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("percentage()")
    class Percentage {

        @Test
        @DisplayName("11% of 1,500,000 = 165,000 (PPN-11 case)")
        void elevenPercentOfFifteenHundredThousand() {
            assertThat(Money.of(1_500_000L).percentage(11).amount())
                    .isEqualByComparingTo(BigDecimal.valueOf(165_000));
        }

        @Test
        @DisplayName("0% returns zero")
        void zeroPercentReturnsZero() {
            assertThat(Money.of(500_000L).percentage(0).amount())
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("100% returns the original amount")
        void oneHundredPercentReturnsOriginal() {
            assertThat(Money.of(500_000L).percentage(100).amount())
                    .isEqualByComparingTo(BigDecimal.valueOf(500_000));
        }

        @Test
        @DisplayName("fractional result is rounded half-up to 2 decimal places")
        void fractionalResultIsRoundedHalfUp() {
            // 11% of 1 = 0.11 → stays 0.11
            assertThat(Money.of(1L).percentage(11).amount())
                    .isEqualByComparingTo(new BigDecimal("0.11"));
        }
    }

    // ── isGreaterThan ────────────────────────────────────────────────────

    @Nested
    @DisplayName("isGreaterThan()")
    class IsGreaterThan {

        @Test
        @DisplayName("returns true when this > other")
        void trueWhenGreater() {
            assertThat(Money.of(200_000L).isGreaterThan(Money.of(100_000L))).isTrue();
        }

        @Test
        @DisplayName("returns false when this == other")
        void falseWhenEqual() {
            assertThat(Money.of(100_000L).isGreaterThan(Money.of(100_000L))).isFalse();
        }

        @Test
        @DisplayName("returns false when this < other")
        void falseWhenLess() {
            assertThat(Money.of(50_000L).isGreaterThan(Money.of(100_000L))).isFalse();
        }
    }

    // ── isZero ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isZero()")
    class IsZero {

        @Test
        @DisplayName("returns true for Money.zero()")
        void trueForZero() {
            assertThat(Money.zero().isZero()).isTrue();
        }

        @Test
        @DisplayName("returns true for of(0)")
        void trueForOfZero() {
            assertThat(Money.of(0L).isZero()).isTrue();
        }

        @Test
        @DisplayName("returns false for a positive amount")
        void falseForPositive() {
            assertThat(Money.of(1L).isZero()).isFalse();
        }
    }
}
