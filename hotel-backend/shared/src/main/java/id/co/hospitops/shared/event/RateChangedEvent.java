package id.co.hospitops.shared.event;

import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.RoomTypeId;
import lombok.Getter;

import java.time.LocalDate;

/**
 * Published when a room type's rate changes for a date range (e.g. a calendar
 * rate edit / new override). The channel module reacts to push the new ARI to
 * connected OTAs. Dates are inclusive.
 */
@Getter
public class RateChangedEvent extends DomainEvent {
    private final HotelId hotelId;
    private final RoomTypeId roomTypeId;
    private final LocalDate from;
    private final LocalDate to;

    public RateChangedEvent(HotelId hotelId, RoomTypeId roomTypeId, LocalDate from, LocalDate to) {
        super();
        this.hotelId = hotelId;
        this.roomTypeId = roomTypeId;
        this.from = from;
        this.to = to;
    }
}
