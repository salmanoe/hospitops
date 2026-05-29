package id.co.hospitops.shared.event;

import id.co.hospitops.shared.HotelId;
import lombok.Getter;

/**
 * Published when a GROUP_ADMIN suspends an active hotel, transitioning it
 * from {@code ACTIVE} to {@code SUSPENDED} status.
 *
 * <p>Listeners use this event to grey out the hotel on the group dashboard
 * and block staff login for that hotel.
 */
@Getter
public class HotelSuspendedEvent extends DomainEvent {
    private final HotelId hotelId;

    public HotelSuspendedEvent(HotelId hotelId) {
        super();
        this.hotelId = hotelId;
    }
}
