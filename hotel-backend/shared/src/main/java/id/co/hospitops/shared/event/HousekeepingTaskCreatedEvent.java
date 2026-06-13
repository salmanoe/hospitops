package id.co.hospitops.shared.event;

import id.co.hospitops.shared.*;
import lombok.Getter;

import java.util.UUID;

@Getter
public class HousekeepingTaskCreatedEvent extends DomainEvent {
    private final HotelId hotelId;
    private final UUID taskId;
    private final RoomId roomId;

    public HousekeepingTaskCreatedEvent(HotelId hotelId, UUID taskId, RoomId roomId) {
        super();
        this.hotelId = hotelId;
        this.taskId = taskId;
        this.roomId = roomId;
    }
}
