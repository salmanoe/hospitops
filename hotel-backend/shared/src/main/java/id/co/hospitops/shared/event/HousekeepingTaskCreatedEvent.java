package id.co.hospitops.shared.event;

import id.co.hospitops.shared.*;
import lombok.Getter;

import java.util.UUID;

@Getter
public class HousekeepingTaskCreatedEvent extends DomainEvent {
    private final UUID taskId;
    private final RoomId roomId;

    public HousekeepingTaskCreatedEvent(UUID taskId, RoomId roomId) {
        super();
        this.taskId = taskId;
        this.roomId = roomId;
    }
}
