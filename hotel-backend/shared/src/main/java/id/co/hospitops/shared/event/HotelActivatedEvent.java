package id.co.hospitops.shared.event;

import id.co.hospitops.shared.HotelId;
import lombok.Getter;

/**
 * Published when a hotel completes its setup checklist and transitions
 * from {@code SETUP} to {@code ACTIVE} status.
 *
 * <p>Listeners use this event to initialise the {@code hotel_summary} row
 * and make the hotel visible on the group dashboard.
 */
@Getter
public class HotelActivatedEvent extends DomainEvent {
    private final HotelId hotelId;

    public HotelActivatedEvent(HotelId hotelId) {
        super();
        this.hotelId = hotelId;
    }
}
