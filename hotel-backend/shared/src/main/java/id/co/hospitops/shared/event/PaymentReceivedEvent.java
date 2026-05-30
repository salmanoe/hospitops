package id.co.hospitops.shared.event;

import id.co.hospitops.shared.*;
import lombok.Getter;

@Getter
public class PaymentReceivedEvent extends DomainEvent {
    private final HotelId hotelId;
    private final InvoiceId invoiceId;
    private final ReservationId reservationId;
    private final Money amount;
    private final boolean fullyPaid;

    public PaymentReceivedEvent(HotelId hotelId,
                                InvoiceId invoiceId,
                                ReservationId reservationId,
                                Money amount, boolean fullyPaid) {
        super();
        this.hotelId = hotelId;
        this.invoiceId = invoiceId;
        this.reservationId = reservationId;
        this.amount = amount;
        this.fullyPaid = fullyPaid;
    }
}
