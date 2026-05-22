package id.co.hospitops.reservation.infrastructure.adapter;

import id.co.hospitops.guest.domain.port.in.ManageGuestUseCase;
import id.co.hospitops.reservation.domain.port.out.GuestValidationPort;
import id.co.hospitops.shared.GuestId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * W-1 FIX: Replaced exception-as-control-flow with a direct {@code existsById} query.
 * W-2 FIX: Now injects {@link ManageGuestUseCase} (the port interface) rather than
 * the concrete {@code GuestService} class, preserving the dependency-inversion rule.
 */
@Component
@RequiredArgsConstructor
public class GuestValidationAdapter implements GuestValidationPort {

    private final ManageGuestUseCase guestUseCase;

    @Override
    public boolean exists(GuestId id) {
        return guestUseCase.existsById(id);
    }
}
