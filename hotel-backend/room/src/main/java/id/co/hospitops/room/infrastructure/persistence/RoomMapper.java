package id.co.hospitops.room.infrastructure.persistence;

import id.co.hospitops.room.domain.model.*;
import id.co.hospitops.room.infrastructure.persistence.entity.*;
import id.co.hospitops.shared.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR,
        imports = {Money.class, RoomId.class, RoomTypeId.class, RoomStatus.class, HotelId.class})
public interface RoomMapper {

    @Mapping(target = "id", expression = "java(rt.getId().value())")
    @Mapping(target = "basePrice", expression = "java(rt.getBasePrice().amount())")
    @Mapping(target = "hotelId", expression = "java(rt.getHotelId().value())")
    RoomTypeJpaEntity toJpa(RoomType rt);

    default RoomType toDomain(RoomTypeJpaEntity e) {
        return RoomType.reconstitute(
                RoomTypeId.of(e.getId()),
                e.getName(),
                e.getCapacity(),
                e.getDescription(),
                Money.of(e.getBasePrice()),
                HotelId.of(e.getHotelId()),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }

    @Mapping(target = "roomTypeId", expression = "java(o.roomTypeId().value())")
    @Mapping(target = "priceOverride", expression = "java(o.priceOverride().amount())")
    RoomRateOverrideJpaEntity toJpa(RoomRateOverride o);

    default RoomRateOverride toDomain(RoomRateOverrideJpaEntity e) {
        return new RoomRateOverride(e.getId(), RoomTypeId.of(e.getRoomTypeId()), e.getName(),
                Money.of(e.getPriceOverride()), e.getValidFrom(), e.getValidUntil());
    }

    @Mapping(target = "id", expression = "java(room.getId().value())")
    @Mapping(target = "roomTypeId", expression = "java(room.getRoomTypeId().value())")
    @Mapping(target = "status", expression = "java(room.getStatus())")
    @Mapping(target = "hotelId", expression = "java(room.getHotelId().value())")
    RoomJpaEntity toJpa(Room room);

    default Room toDomain(RoomJpaEntity e) {
        return Room.reconstitute(
                RoomId.of(e.getId()),
                e.getRoomNumber(),
                e.getFloor(),
                e.getStatus(),
                RoomTypeId.of(e.getRoomTypeId()),
                e.getNotes(),
                HotelId.of(e.getHotelId()),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
