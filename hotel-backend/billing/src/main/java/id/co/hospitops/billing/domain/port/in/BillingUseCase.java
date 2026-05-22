package id.co.hospitops.billing.domain.port.in;

import id.co.hospitops.billing.application.command.RecordPaymentCommand;
import id.co.hospitops.billing.application.response.InvoiceResponse;
import id.co.hospitops.shared.InvoiceId;
import id.co.hospitops.shared.ReservationId;
import id.co.hospitops.shared.web.PageResult;
import org.springframework.data.domain.Pageable;

public interface BillingUseCase {
    InvoiceResponse findById(InvoiceId id);

    PageResult<InvoiceResponse> findAll(String statusFilter, Pageable pageable);

    InvoiceResponse recordPayment(InvoiceId id, RecordPaymentCommand command);

    byte[] generatePdf(InvoiceId id);

    /**
     * Auto-generate an invoice when a reservation is checked out.
     *
     * <p>Exposed as a port-in method so that {@link id.co.hospitops.billing.application.BillingEventHandler}
     * can delegate here without directly depending on {@link id.co.hospitops.billing.application.BillingService}.
     * Callers should not invoke this method directly — it is driven by
     * {@link id.co.hospitops.shared.event.ReservationCheckedOutEvent}.
     *
     * @param reservationId the checked-out reservation
     * @param nights        number of nights stayed
     */
    void createInvoiceForCheckout(ReservationId reservationId, long nights);
}
