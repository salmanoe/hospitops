package id.co.hospitops.reservation.domain.port.out;

import id.co.hospitops.shared.GuestId;

public interface GuestValidationPort {
    boolean exists(GuestId guestId);
}
