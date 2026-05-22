package id.co.hospitops.room.infrastructure.persistence;

import id.co.hospitops.room.domain.model.RoomRateOverride;
import id.co.hospitops.room.domain.port.out.RoomRateOverrideRepository;
import id.co.hospitops.room.infrastructure.persistence.entity.RoomRateOverrideJpaEntity;
import id.co.hospitops.shared.Money;
import id.co.hospitops.shared.RoomTypeId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Currency;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class RoomRateOverrideRepositoryImpl implements RoomRateOverrideRepository {

    private final RoomRateOverrideJpaRepository jpa;

    @Override
    public RoomRateOverride save(RoomRateOverride override) {
        return toDomain(jpa.save(toJpa(override)));
    }

    @Override
    public List<RoomRateOverride> findByRoomTypeId(RoomTypeId roomTypeId) {
        return jpa.findByRoomTypeId(roomTypeId.value())
                .stream().map(this::toDomain).toList();
    }

    // ── Mapping ─────────────────────────────────────────────────────────

    private RoomRateOverrideJpaEntity toJpa(RoomRateOverride o) {
        return RoomRateOverrideJpaEntity.builder()
                .id(o.id())
                .roomTypeId(o.roomTypeId().value())
                .name(o.name())
                .priceOverride(o.priceOverride().amount())
                .validFrom(o.validFrom())
                .validUntil(o.validUntil())
                .build();
    }

    private RoomRateOverride toDomain(RoomRateOverrideJpaEntity e) {
        return new RoomRateOverride(
                e.getId(),
                RoomTypeId.of(e.getRoomTypeId()),
                e.getName(),
                new Money(e.getPriceOverride(), Currency.getInstance("IDR")),
                e.getValidFrom(),
                e.getValidUntil());
    }
}
