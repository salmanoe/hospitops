package id.co.hospitops.room.domain.model;

import org.hibernate.annotations.Struct;

@Struct(name = "room_status")
public enum RoomStatus {
    AVAILABLE, OCCUPIED, DIRTY, MAINTENANCE;

    public boolean isAvailableForBooking() {
        return this == AVAILABLE;
    }
}
