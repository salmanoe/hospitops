package id.co.hospitops.channel.infrastructure.persistence;

import id.co.hospitops.channel.domain.model.ChannelRoomTypeMapping;
import id.co.hospitops.channel.domain.port.out.ChannelRoomTypeMappingRepository;
import id.co.hospitops.channel.infrastructure.persistence.entity.ChannelRoomTypeMappingJpaEntity;
import id.co.hospitops.shared.ChannelRoomTypeMappingId;
import id.co.hospitops.shared.HotelContext;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.RoomTypeId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ChannelRoomTypeMappingRepositoryImpl implements ChannelRoomTypeMappingRepository {

    private final ChannelRoomTypeMappingJpaRepository jpa;

    @Override
    public ChannelRoomTypeMapping save(ChannelRoomTypeMapping mapping) {
        // Copy onto the already-managed entity when present (see
        // ChannelPropertyMappingRepositoryImpl#save for the rationale).
        ChannelRoomTypeMappingJpaEntity entity = jpa.findById(mapping.getId().value())
                .map(existing -> {
                    existing.setExternalRoomTypeId(mapping.getExternalRoomTypeId());
                    existing.setExternalRatePlanId(mapping.getExternalRatePlanId());
                    return existing;
                })
                .orElseGet(() -> toJpa(mapping));
        return toDomain(jpa.save(entity));
    }

    @Override
    public Optional<ChannelRoomTypeMapping> findByRoomTypeId(RoomTypeId roomTypeId) {
        return jpa.findByHotelIdAndRoomTypeId(HotelContext.current().value(), roomTypeId.value())
                .map(this::toDomain);
    }

    @Override
    public List<ChannelRoomTypeMapping> findAll() {
        return jpa.findByHotelId(HotelContext.current().value())
                .stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByRoomTypeId(RoomTypeId roomTypeId) {
        return jpa.existsByHotelIdAndRoomTypeId(HotelContext.current().value(), roomTypeId.value());
    }

    // ── Mapping ─────────────────────────────────────────────────────────

    private ChannelRoomTypeMappingJpaEntity toJpa(ChannelRoomTypeMapping m) {
        return ChannelRoomTypeMappingJpaEntity.builder()
                .id(m.getId().value())
                .hotelId(m.getHotelId().value())
                .roomTypeId(m.getRoomTypeId().value())
                .externalRoomTypeId(m.getExternalRoomTypeId())
                .externalRatePlanId(m.getExternalRatePlanId())
                .build();
    }

    private ChannelRoomTypeMapping toDomain(ChannelRoomTypeMappingJpaEntity e) {
        return ChannelRoomTypeMapping.reconstitute(
                ChannelRoomTypeMappingId.of(e.getId()),
                HotelId.of(e.getHotelId()),
                RoomTypeId.of(e.getRoomTypeId()),
                e.getExternalRoomTypeId(),
                e.getExternalRatePlanId(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
