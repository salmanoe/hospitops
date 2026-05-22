package id.co.hospitops.room.application.response;

import id.co.hospitops.room.domain.model.RoomType;
import id.co.hospitops.shared.RoomTypeId;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RoomTypeResponse(
        RoomTypeId    id,
        String        name,
        int           capacity,
        String        description,
        BigDecimal    basePrice,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static RoomTypeResponse from(RoomType rt) {
        return new RoomTypeResponse(
                rt.getId(), rt.getName(), rt.getCapacity(),
                rt.getDescription(), rt.getBasePrice().amount(),
                rt.getCreatedAt(), rt.getUpdatedAt());
    }
}
