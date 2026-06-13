package id.co.hospitops.channel.domain.model;

import id.co.hospitops.shared.ChannelRoomTypeMappingId;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.RoomTypeId;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Links a HospitOps {@link RoomTypeId} to the provider's room type and rate
 * plan. Outbound ARI uses it to know where to push availability/rates;
 * inbound OTA bookings use it to resolve which room type was sold.
 */
@Getter
public class ChannelRoomTypeMapping {

    private final ChannelRoomTypeMappingId id;
    private final HotelId hotelId;
    private final RoomTypeId roomTypeId;
    private String externalRoomTypeId;
    private String externalRatePlanId;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ChannelRoomTypeMapping create(HotelId hotelId, RoomTypeId roomTypeId,
                                                String externalRoomTypeId, String externalRatePlanId) {
        validate(externalRoomTypeId, externalRatePlanId);
        return new ChannelRoomTypeMapping(
                ChannelRoomTypeMappingId.generate(), hotelId, roomTypeId,
                externalRoomTypeId, externalRatePlanId,
                LocalDateTime.now(), LocalDateTime.now());
    }

    public static ChannelRoomTypeMapping reconstitute(ChannelRoomTypeMappingId id, HotelId hotelId,
                                                      RoomTypeId roomTypeId, String externalRoomTypeId,
                                                      String externalRatePlanId, LocalDateTime createdAt,
                                                      LocalDateTime updatedAt) {
        return new ChannelRoomTypeMapping(id, hotelId, roomTypeId, externalRoomTypeId,
                externalRatePlanId, createdAt, updatedAt);
    }

    private ChannelRoomTypeMapping(ChannelRoomTypeMappingId id, HotelId hotelId, RoomTypeId roomTypeId,
                                   String externalRoomTypeId, String externalRatePlanId,
                                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.hotelId = hotelId;
        this.roomTypeId = roomTypeId;
        this.externalRoomTypeId = externalRoomTypeId;
        this.externalRatePlanId = externalRatePlanId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void update(String externalRoomTypeId, String externalRatePlanId) {
        validate(externalRoomTypeId, externalRatePlanId);
        this.externalRoomTypeId = externalRoomTypeId;
        this.externalRatePlanId = externalRatePlanId;
        this.updatedAt = LocalDateTime.now();
    }

    private static void validate(String externalRoomTypeId, String externalRatePlanId) {
        if (externalRoomTypeId == null || externalRoomTypeId.isBlank())
            throw new IllegalArgumentException("External room type id cannot be blank");
        if (externalRatePlanId == null || externalRatePlanId.isBlank())
            throw new IllegalArgumentException("External rate plan id cannot be blank");
    }
}
