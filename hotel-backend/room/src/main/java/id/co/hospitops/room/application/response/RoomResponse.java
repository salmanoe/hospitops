package id.co.hospitops.room.application.response;

import id.co.hospitops.room.domain.model.Room;
import id.co.hospitops.room.domain.model.RoomStatus;
import id.co.hospitops.room.domain.model.RoomType;
import id.co.hospitops.shared.RoomId;
import id.co.hospitops.shared.RoomTypeId;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RoomResponse(
        RoomId        id,
        String        roomNumber,
        int           floor,
        RoomStatus    status,
        RoomTypeId    roomTypeId,
        String        roomTypeName,
        int           capacity,
        BigDecimal    basePrice,
        String        notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static RoomResponse from(Room room, RoomType roomType) {
        return new RoomResponse(
                room.getId(), room.getRoomNumber(), room.getFloor(),
                room.getStatus(), room.getRoomTypeId(),
                roomType.getName(), roomType.getCapacity(),
                roomType.getBasePrice().amount(),
                room.getNotes(), room.getCreatedAt(), room.getUpdatedAt());
    }
}
