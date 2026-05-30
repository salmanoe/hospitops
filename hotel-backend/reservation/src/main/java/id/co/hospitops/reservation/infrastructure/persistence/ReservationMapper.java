package id.co.hospitops.reservation.infrastructure.persistence;

import id.co.hospitops.reservation.domain.model.*;
import id.co.hospitops.reservation.infrastructure.persistence.entity.ReservationJpaEntity;
import id.co.hospitops.shared.*;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        imports = {ReservationId.class, GuestId.class, RoomId.class,
                StaffId.class, Money.class, HotelId.class}
)
public interface ReservationMapper {

    @Mapping(target = "id", expression = "java(r.getId().value())")
    @Mapping(target = "guestId", expression = "java(r.getGuestId().value())")
    @Mapping(target = "roomId", expression = "java(r.getRoomId().value())")
    @Mapping(target = "createdBy", expression = "java(r.getCreatedBy() != null ? r.getCreatedBy().value() : null)")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "ratePerNight", expression = "java(r.getRatePerNight().amount())")
    @Mapping(target = "hotelId", expression = "java(r.getHotelId().value())")
    ReservationJpaEntity toJpa(Reservation r);

    default Reservation toDomain(ReservationJpaEntity e) {
        return Reservation.reconstitute(
                ReservationId.of(e.getId()),
                e.getReservationNumber(),
                GuestId.of(e.getGuestId()),
                RoomId.of(e.getRoomId()),
                e.getCheckInDate(),
                e.getCheckOutDate(),
                e.getStatus(),
                Money.of(e.getRatePerNight()),
                e.getAdults(),
                e.getChildren(),
                e.getSpecialRequests(),
                e.getCreatedBy() != null ? StaffId.of(e.getCreatedBy()) : null,
                HotelId.of(e.getHotelId()),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
