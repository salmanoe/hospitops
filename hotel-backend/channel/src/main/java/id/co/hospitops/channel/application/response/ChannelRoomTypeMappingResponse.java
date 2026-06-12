package id.co.hospitops.channel.application.response;

import id.co.hospitops.channel.domain.model.ChannelRoomTypeMapping;
import id.co.hospitops.shared.ChannelRoomTypeMappingId;
import id.co.hospitops.shared.RoomTypeId;

import java.time.LocalDateTime;

public record ChannelRoomTypeMappingResponse(
        ChannelRoomTypeMappingId id,
        RoomTypeId               roomTypeId,
        String                   externalRoomTypeId,
        String                   externalRatePlanId,
        LocalDateTime            createdAt,
        LocalDateTime            updatedAt
) {
    public static ChannelRoomTypeMappingResponse from(ChannelRoomTypeMapping m) {
        return new ChannelRoomTypeMappingResponse(
                m.getId(), m.getRoomTypeId(), m.getExternalRoomTypeId(),
                m.getExternalRatePlanId(), m.getCreatedAt(), m.getUpdatedAt());
    }
}
