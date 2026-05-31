package id.co.hospitops.billing.domain.model;

import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.InvoiceId;
import id.co.hospitops.shared.Money;
import id.co.hospitops.shared.ReservationId;
import id.co.hospitops.shared.StaffId;
import id.co.hospitops.shared.TaxPolicy;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class Invoice {

    private final InvoiceId id;
    private final HotelId hotelId;
    private final String invoiceNumber;
    private final ReservationId reservationId;
    private final String reservationNumber;
    private final String guestName;
    private final List<InvoiceItem> items;
    private final List<Payment> payments;
    private final Money subtotal;
    private final Money taxAmount;
    private Money discountAmount;
    private Money totalAmount;
    private PaymentStatus paymentStatus;
    private final LocalDate dueDate;
    private final String notes;
    private final LocalDateTime issuedAt;
    private LocalDateTime updatedAt;

    // ── Factories ───────────────────────────────────────────────

    /**
     * Creates a new invoice for a checkout.
     *
     * @param taxPolicy the tax strategy resolved from the hotel's policy config via
     *                  {@link id.co.hospitops.billing.domain.port.out.HotelPolicyPort}.
     *                  Previously this was a hardcoded static constant (PPN_11); it is
     *                  now supplied by the caller so the rate is hotel-specific and
     *                  configurable at runtime without a code change.
     */
    public static Invoice create(HotelId hotelId, String invoiceNumber,
                                 ReservationId reservationId,
                                 String reservationNumber, String guestName,
                                 long nights, Money ratePerNight, String roomTypeName,
                                 TaxPolicy taxPolicy) {
        List<InvoiceItem> items = new ArrayList<>();
        Money subtotal = ratePerNight.multiply((int) nights);
        items.add(new InvoiceItem(UUID.randomUUID(),
                roomTypeName + " — " + nights + " night(s)",
                (int) nights, ratePerNight, subtotal));

        Money tax = taxPolicy.calculate(subtotal);
        Money discount = Money.zero();
        Money total = subtotal.add(tax);

        return new Invoice(InvoiceId.generate(), hotelId, invoiceNumber, reservationId,
                reservationNumber, guestName, items, new ArrayList<>(), subtotal, tax, discount,
                total, PaymentStatus.UNPAID, null, null, LocalDateTime.now(), LocalDateTime.now());
    }

    public static Invoice reconstitute(InvoiceId id, HotelId hotelId, String invoiceNumber,
                                       ReservationId reservationId, String reservationNumber,
                                       String guestName,
                                       List<InvoiceItem> items, List<Payment> payments,
                                       Money subtotal, Money taxAmount,
                                       Money discountAmount, Money totalAmount,
                                       PaymentStatus paymentStatus, LocalDate dueDate,
                                       String notes, LocalDateTime issuedAt,
                                       LocalDateTime updatedAt) {
        return new Invoice(id, hotelId, invoiceNumber, reservationId, reservationNumber, guestName,
                items, payments, subtotal, taxAmount, discountAmount, totalAmount,
                paymentStatus, dueDate, notes, issuedAt, updatedAt);
    }

    private Invoice(InvoiceId id, HotelId hotelId, String invoiceNumber,
                    ReservationId reservationId,
                    String reservationNumber, String guestName,
                    List<InvoiceItem> items, List<Payment> payments,
                    Money subtotal, Money taxAmount, Money discountAmount,
                    Money totalAmount, PaymentStatus paymentStatus, LocalDate dueDate,
                    String notes, LocalDateTime issuedAt, LocalDateTime updatedAt) {
        this.id = id;
        this.hotelId = hotelId;
        this.invoiceNumber = invoiceNumber;
        this.reservationId = reservationId;
        this.reservationNumber = reservationNumber;
        this.guestName = guestName;
        this.items = items;
        this.payments = payments;
        this.subtotal = subtotal;
        this.taxAmount = taxAmount;
        this.discountAmount = discountAmount;
        this.totalAmount = totalAmount;
        this.paymentStatus = paymentStatus;
        this.dueDate = dueDate;
        this.notes = notes;
        this.issuedAt = issuedAt;
        this.updatedAt = updatedAt;
    }

    // ── Business rules ──────────────────────────────────────────
    public void recordPayment(Money amount, PaymentMethod method,
                              String referenceNo, StaffId receivedBy) {
        if (paymentStatus == PaymentStatus.PAID)
            throw new IllegalStateException("Invoice is already fully paid");

        // R-12 FIX: Reject payments that would exceed the outstanding balance.
        // Previously, overpayment was silently accepted and the status was
        // recalculated as PAID — but the excess was not tracked or refunded.
        // Callers must split payments or record the exact balance due.
        Money newTotal = getTotalPaid().add(amount);
        if (newTotal.amount().compareTo(totalAmount.amount()) > 0)
            throw new IllegalStateException(
                    "Payment of " + amount.amount() +
                            " would exceed invoice total " + totalAmount.amount() +
                            " (outstanding balance: " + getBalance().amount() + ")");

        Payment payment = new Payment(UUID.randomUUID(), id, amount, method,
                referenceNo, receivedBy, LocalDateTime.now());
        this.payments.add(payment);
        recalculateStatus();
        this.updatedAt = LocalDateTime.now();
    }

    public void applyDiscount(Money discount) {
        this.discountAmount = discount;
        this.totalAmount = subtotal.add(taxAmount).subtract(discount);
        recalculateStatus();
        this.updatedAt = LocalDateTime.now();
    }

    public Money getTotalPaid() {
        return payments.stream()
                .map(Payment::amount)
                .reduce(Money.zero(), Money::add);
    }

    public Money getBalance() {
        return totalAmount.subtract(getTotalPaid());
    }

    private void recalculateStatus() {
        Money paid = getTotalPaid();
        if (paid.amount().compareTo(totalAmount.amount()) >= 0)
            paymentStatus = PaymentStatus.PAID;
        else if (paid.isZero())
            paymentStatus = PaymentStatus.UNPAID;
        else
            paymentStatus = PaymentStatus.PARTIAL;
    }
}
