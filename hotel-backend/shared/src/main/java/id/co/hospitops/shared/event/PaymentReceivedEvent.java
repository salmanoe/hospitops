package id.co.hospitops.shared.event;

import id.co.hospitops.shared.*;

public class PaymentReceivedEvent extends DomainEvent {
    private final InvoiceId invoiceId;
    private final ReservationId reservationId;
    private final Money amount;
    private final boolean fullyPaid;

    public PaymentReceivedEvent(Object source, InvoiceId invoiceId,
                                ReservationId reservationId,
                                Money amount, boolean fullyPaid) {
        super(source);
        this.invoiceId = invoiceId;
        this.reservationId = reservationId;
        this.amount = amount;
        this.fullyPaid = fullyPaid;
    }

    public InvoiceId getInvoiceId() {
        return invoiceId;
    }

    public ReservationId getReservationId() {
        return reservationId;
    }

    public Money getAmount() {
        return amount;
    }

    public boolean isFullyPaid() {
        return fullyPaid;
    }
}
