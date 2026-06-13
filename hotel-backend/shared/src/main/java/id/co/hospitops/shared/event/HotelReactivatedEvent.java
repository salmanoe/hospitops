package id.co.hospitops.shared.event;

import id.co.hospitops.shared.HotelId;
import lombok.Getter;

/**
 * Published when a GROUP_ADMIN reactivates a suspended hotel, transitioning it
 * from {@code SUSPENDED} back to {@code ACTIVE} status.
 *
 * <p>Listeners can use this event to re-enable hotel operations on the group
 * dashboard and re-allow staff login for that hotel.
 */
@Getter
public class HotelReactivatedEvent extends DomainEvent {
    private final HotelId hotelId;

    public HotelReactivatedEvent(HotelId hotelId) {
        super();
        this.hotelId = hotelId;
    }
}
