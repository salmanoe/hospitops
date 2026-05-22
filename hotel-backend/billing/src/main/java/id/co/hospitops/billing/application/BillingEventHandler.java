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
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingEventHandler {

    private final BillingUseCase billingUseCase;

    /**
     * Triggers invoice generation when a reservation is checked out.
     *
     * <p>Delegates entirely to {@link BillingUseCase#createInvoiceForCheckout}
     * — no business logic lives here.
     */
    @EventListener
    @Transactional
    public void onCheckout(ReservationCheckedOutEvent event) {
        log.debug("BillingEventHandler received checkout event for reservation {}",
                event.getReservationId());
        billingUseCase.createInvoiceForCheckout(
                event.getReservationId(), event.getNights());
    }
}
