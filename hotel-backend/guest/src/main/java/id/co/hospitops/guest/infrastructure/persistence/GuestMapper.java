package id.co.hospitops.guest.infrastructure.persistence;

// R-17 FIX: Extracted the toJpa/toDomain conversion logic from GuestRepositoryImpl
// into a dedicated @Component. Benefits:
//   - Testable in isolation (GuestMapperTest) without any Spring Data wiring
//   - Consistent with the pattern used by RoomMapper in the room module
//   - Removes the temptation to copy-paste the mapping into future adapters

import id.co.hospitops.guest.domain.model.Guest;
import id.co.hospitops.guest.infrastructure.persistence.entity.GuestJpaEntity;
import id.co.hospitops.shared.GuestId;
import id.co.hospitops.shared.HotelId;
import org.springframework.stereotype.Component;

@Component
public class GuestMapper {

    public GuestJpaEntity toJpa(Guest g) {
        return GuestJpaEntity.builder()
                .id(g.getId().value())
                .fullName(g.getFullName())
                .idNumber(g.getIdNumber())
                .nationality(g.getNationality())
                .phone(g.getPhone())
                .email(g.getEmail())
                .address(g.getAddress())
                .hotelId(g.getHotelId().value())
                .createdAt(g.getCreatedAt())
                .updatedAt(g.getUpdatedAt())
                .build();
    }

    public Guest toDomain(GuestJpaEntity e) {
        return Guest.reconstitute(
                GuestId.of(e.getId()),
                e.getFullName(),
                e.getIdNumber(),
                e.getNationality(),
                e.getPhone(),
                e.getEmail(),
                e.getAddress(),
                HotelId.of(e.getHotelId()),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
