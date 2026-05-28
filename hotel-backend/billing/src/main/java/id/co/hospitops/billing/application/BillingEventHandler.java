package id.co.hospitops.billing.application;

// R3-03 FIX: Extracted from BillingService to restore Single Responsibility.
//
// Previously BillingService was both a @Service (business logic) AND an
// @EventListener (event routing). Mixing these two roles makes the service
// harder to test and violates SRP.
//
// This class is the sole owner of the @EventListener concern for the billing
// module, mirroring the pattern used by HousekeepingEventListener. It holds no
// business logic of its own — it simply maps domain events to BillingUseCase
// method calls.

import id.co.hospitops.billing.domain.port.in.BillingUseCase;
import id.co.hospitops.shared.event.ReservationCheckedOutEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingEventHandler {

    private final BillingUseCase billingUseCase;

    /**
     * Triggers invoice generation when a reservation is checked out.
     *
     * <p>R-01 FIX: Runs AFTER the checkout transaction commits so invoice creation
     * never rolls back the checkout itself. Delegates entirely to
     * {@link BillingUseCase#createInvoiceForCheckout} — no business logic here.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCheckout(ReservationCheckedOutEvent event) {
        log.debug("BillingEventHandler received checkout event for reservation {}",
                event.getReservationId());
        billingUseCase.createInvoiceForCheckout(
                event.getReservationId(), event.getNights());
    }
}
