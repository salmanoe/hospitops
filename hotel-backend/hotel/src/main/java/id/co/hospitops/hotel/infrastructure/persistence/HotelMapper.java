package id.co.hospitops.hotel.infrastructure.persistence;

import id.co.hospitops.hotel.domain.model.Hotel;
import id.co.hospitops.hotel.domain.model.SetupChecklist;
import id.co.hospitops.hotel.infrastructure.persistence.entity.HotelJpaEntity;
import id.co.hospitops.hotel.infrastructure.persistence.entity.SetupChecklistJpaEntity;
import id.co.hospitops.shared.GroupId;
import id.co.hospitops.shared.HotelId;
import org.springframework.stereotype.Component;

@Component
class HotelMapper {

    HotelJpaEntity toJpa(Hotel h) {
        return HotelJpaEntity.builder()
                .id(h.getId().value())
                .groupId(h.getGroupId().value())
                .name(h.getName())
                .address(h.getAddress())
                .timezone(h.getTimezone())
                .currency(h.getCurrency())
                .starRating(h.getStarRating())
                .defaultCheckInTime(h.getDefaultCheckInTime())
                .defaultCheckOutTime(h.getDefaultCheckOutTime())
                .status(h.getStatus())
                .build();
    }

    Hotel toDomain(HotelJpaEntity e, SetupChecklistJpaEntity c) {
        // SetupChecklist is a value object — hotelId is supplied by the mapper (owner),
        // not carried by the checklist itself.
        SetupChecklist checklist = SetupChecklist.reconstitute(
                c.isProfileComplete(),
                c.isPolicyComplete(),
                c.isRoomTypeAdded(),
                c.isRoomAdded(),
                c.isStaffAccountCreated());

        return Hotel.reconstitute(
                HotelId.of(e.getId()),
                GroupId.of(e.getGroupId()),
                e.getName(),
                e.getAddress(),
                e.getTimezone(),
                e.getCurrency(),
                e.getStarRating(),
                e.getDefaultCheckInTime(),
                e.getDefaultCheckOutTime(),
                e.getStatus(),
                checklist,
                e.getCreatedAt(),
                e.getUpdatedAt());
    }

    /**
     * @param hotelId the owning hotel's ID — the checklist does not carry its own identity,
     *                so the mapper supplies it when building the JPA entity.
     */
    SetupChecklistJpaEntity toChecklistJpa(HotelId hotelId, SetupChecklist c) {
        return SetupChecklistJpaEntity.builder()
                .hotelId(hotelId.value())
                .profileComplete(c.isProfileComplete())
                .policyComplete(c.isPolicyComplete())
                .roomTypeAdded(c.isRoomTypeAdded())
                .roomAdded(c.isRoomAdded())
                .staffAccountCreated(c.isStaffAccountCreated())
                .build();
    }
}
