package id.co.hospitops.shared.event;

import id.co.hospitops.shared.*;
import lombok.Getter;

@Getter
public class RoomCreatedEvent extends DomainEvent {
    private final HotelId hotelId;
    private final RoomId roomId;

    public RoomCreatedEvent(HotelId hotelId, RoomId roomId) {
        super();
        this.hotelId = hotelId;
        this.roomId = roomId;
    }
}
