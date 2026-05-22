package id.co.hospitops.shared.event;

import id.co.hospitops.shared.*;

import java.util.UUID;

public class HousekeepingTaskCreatedEvent extends DomainEvent {
    private final UUID taskId;
    private final RoomId roomId;

    public HousekeepingTaskCreatedEvent(Object source, UUID taskId, RoomId roomId) {
        super(source);
        this.taskId = taskId;
        this.roomId = roomId;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public RoomId getRoomId() {
        return roomId;
    }
}
