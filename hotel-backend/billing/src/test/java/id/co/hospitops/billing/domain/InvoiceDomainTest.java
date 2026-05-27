package id.co.hospitops.billing.domain;

import id.co.hospitops.billing.domain.model.Invoice;
import id.co.hospitops.billing.domain.model.PaymentMethod;
import id.co.hospitops.billing.domain.model.PaymentStatus;
import id.co.hospitops.shared.Money;
import id.co.hospitops.shared.ReservationId;
import id.co.hospitops.shared.StaffId;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Invoice payment business rules.
 * <p>
 * Covers the R-12 overpayment guard, R-11 tax calculation,
 * and the full payment-status lifecycle.
 */
@DisplayName("Invoice Domain")
class InvoiceDomainTest {

    /**
     * 3 nights x Rp 500,000 = subtotal 1,500,000; tax 11% = 165,000; total 1,665,000
     */
    private Invoice newInvoice() {
        return Invoice.create("INV-2025-00001", ReservationId.generate(),
                "RES-2025-00001", "Budi Santoso",
                3L, Money.of(500_000L), "Deluxe");
    }

    private StaffId cashier() {
        return StaffId.generate();
    }

    // ── Tax calculation (R-11) ───────────────────────────────────────────

    @Nested
    @DisplayName("Tax calculation (11% PPN)")
    class TaxCalculation {

        @Test
        @DisplayName("subtotal is rate x nights")
        void subtotalCorrect() {
            assertThat(newInvoice().getSubtotal().amount())
                    .isEqualByComparingTo(BigDecimal.valueOf(1_500_000));
        }

        @Test
        @DisplayName("tax is 11% of subtotal, rounded half-up")
        void taxCorrect() {
            assertThat(newInvoice().getTaxAmount().amount())
                    .isEqualByComparingTo(BigDecimal.valueOf(165_000));
        }

        @Test
        @DisplayName("total = subtotal + tax")
        void totalCorrect() {
            assertThat(newInvoice().getTotalAmount().amount())
                    .isEqualByComparingTo(BigDecimal.valueOf(1_665_000));
        }
    }

    // ── Payment status lifecycle ──────────────────────────────────────────

    @Nested
    @DisplayName("Payment status lifecycle")
    class StatusLifecycle {

        @Test
        @DisplayName("new invoice is UNPAID")
        void startsUnpaid() {
            assertThat(newInvoice().getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
        }

        @Test
        @DisplayName("partial payment -> PARTIAL")
        void partialPaymentSetsPartial() {
            Invoice inv = newInvoice();
            inv.recordPayment(Money.of(800_000L), PaymentMethod.CASH, null, cashier());
            assertThat(inv.getPaymentStatus()).isEqualTo(PaymentStatus.PARTIAL);
        }

        @Test
        @DisplayName("exact full payment -> PAID")
        void fullPaymentSetsPaid() {
            Invoice inv = newInvoice();
            inv.recordPayment(Money.of(1_665_000L), PaymentMethod.CASH, null, cashier());
            assertThat(inv.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        }

        @Test
        @DisplayName("split payments summing to total -> PAID")
        void splitPaymentSetsPaid() {
            Invoice inv = newInvoice();
            inv.recordPayment(Money.of(1_000_000L), PaymentMethod.CASH, null, cashier());
            inv.recordPayment(Money.of(665_000L), PaymentMethod.BANK_TRANSFER, "TRF-001", cashier());
            assertThat(inv.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        }
    }

    // ── Overpayment guard (R-12) ─────────────────────────────────────────

    @Nested
    @DisplayName("Overpayment guard (R-12 fix)")
    class OverpaymentGuard {

        @Test
        @DisplayName("payment exceeding total throws with descriptive message")
        void overpaymentThrows() {
            Invoice inv = newInvoice();
            assertThatThrownBy(() ->
                    inv.recordPayment(Money.of(2_000_000L), PaymentMethod.CASH, null, cashier()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("would exceed");
        }

        @Test
        @DisplayName("second payment that would overflow total throws")
        void secondPaymentOverflowThrows() {
            Invoice inv = newInvoice();
            inv.recordPayment(Money.of(1_500_000L), PaymentMethod.CASH, null, cashier());
            // 165,000 remaining; trying to pay 200,000 must fail
            assertThatThrownBy(() ->
                    inv.recordPayment(Money.of(200_000L), PaymentMethod.CASH, null, cashier()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("would exceed");
        }

        @Test
        @DisplayName("payment after fully paid throws 'already fully paid'")
        void paymentAfterPaidThrows() {
            Invoice inv = newInvoice();
            inv.recordPayment(Money.of(1_665_000L), PaymentMethod.CASH, null, cashier());
            assertThatThrownBy(() ->
                    inv.recordPayment(Money.of(1L), PaymentMethod.CASH, null, cashier()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already fully paid");
        }
    }

    // ── Balance and getTotalPaid ─────────────────────────────────────────

    @Nested
    @DisplayName("Balance tracking")
    class BalanceTracking {

        @Test
        @DisplayName("balance starts at total amount")
        void initialBalance() {
            assertThat(newInvoice().getBalance().amount())
                    .isEqualByComparingTo(BigDecimal.valueOf(1_665_000));
        }

        @Test
        @DisplayName("balance decreases after partial payment")
        void balanceAfterPartial() {
            Invoice inv = newInvoice();
            inv.recordPayment(Money.of(1_000_000L), PaymentMethod.CASH, null, cashier());
            assertThat(inv.getBalance().amount())
                    .isEqualByComparingTo(BigDecimal.valueOf(665_000));
        }

        @Test
        @DisplayName("balance is zero after full payment")
        void balanceAfterFull() {
            Invoice inv = newInvoice();
            inv.recordPayment(Money.of(1_665_000L), PaymentMethod.CASH, null, cashier());
            assertThat(inv.getBalance().amount())
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // ── Discount ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Discount application")
    class Discount {

        @Test
        @DisplayName("discount reduces total and allows exact reduced payment")
        void discountReducesTotal() {
            Invoice inv = newInvoice();
            inv.applyDiscount(Money.of(165_000L)); // waive the tax
            assertThat(inv.getTotalAmount().amount())
                    .isEqualByComparingTo(BigDecimal.valueOf(1_500_000));
            // Exact post-discount payment must succeed
            inv.recordPayment(Money.of(1_500_000L), PaymentMethod.CASH, null, cashier());
            assertThat(inv.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        }
    }
}
