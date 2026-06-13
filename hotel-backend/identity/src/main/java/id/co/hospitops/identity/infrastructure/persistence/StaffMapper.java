package id.co.hospitops.identity.infrastructure.persistence;

import id.co.hospitops.identity.domain.model.Staff;
import id.co.hospitops.identity.infrastructure.persistence.entity.StaffJpaEntity;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.StaffId;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
class StaffMapper {

    StaffJpaEntity toJpa(Staff staff) {
        return StaffJpaEntity.builder()
                .id(staff.getId().value())
                .fullName(staff.getFullName())
                .username(staff.getUsername())
                .passwordHash(staff.getPasswordHash())
                .role(staff.getRole())
                .active(staff.isActive())
                .hotelId(requireHotelId(staff.getHotelId()))
                .createdAt(staff.getCreatedAt())
                .updatedAt(staff.getUpdatedAt())
                .build();
    }

    private static UUID requireHotelId(HotelId hotelId) {
        if (hotelId == null) throw new IllegalStateException(
                "Staff.hotelId must not be null — every staff member must be hotel-scoped. " +
                        "This is a programming error: Staff was created without a hotel context.");
        return hotelId.value();
    }

    Staff toDomain(StaffJpaEntity e) {
        return Staff.reconstitute(
                StaffId.of(e.getId()), e.getFullName(), e.getUsername(),
                e.getPasswordHash(), e.getRole(), e.isActive(),
                e.getHotelId() != null ? HotelId.of(e.getHotelId()) : null,
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
