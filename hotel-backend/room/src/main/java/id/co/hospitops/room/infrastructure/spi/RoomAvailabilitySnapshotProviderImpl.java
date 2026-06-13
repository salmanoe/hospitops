package id.co.hospitops.room.infrastructure.spi;

import id.co.hospitops.room.domain.model.RoomType;
import id.co.hospitops.room.domain.port.out.RoomRateOverrideRepository;
import id.co.hospitops.room.domain.port.out.RoomRepository;
import id.co.hospitops.room.domain.port.out.RoomTypeRepository;
import id.co.hospitops.room.infrastructure.persistence.RoomJpaRepository;
import id.co.hospitops.shared.RoomId;
import id.co.hospitops.shared.RoomTypeId;
import id.co.hospitops.shared.channel.RoomAvailabilitySnapshotProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Room-module implementation of the channel availability SPI. Reuses the
 * domain repositories (hotel-scoped) plus a direct sellable-count query.
 */
@Component
@RequiredArgsConstructor
public class RoomAvailabilitySnapshotProviderImpl implements RoomAvailabilitySnapshotProvider {

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRateOverrideRepository overrideRepository;
    private final RoomJpaRepository roomJpa;

    @Override
    public Optional<RoomTypeId> roomTypeOf(RoomId roomId) {
        return roomRepository.findById(roomId).map(r -> r.getRoomTypeId());
    }

    @Override
    public int availableUnits(RoomTypeId roomTypeId, LocalDate night) {
        return Math.toIntExact(roomJpa.countSellableByRoomType(roomTypeId.value(), night));
    }

    @Override
    public BigDecimal ratePerNight(RoomTypeId roomTypeId, LocalDate night) {
        return overrideRepository.findByRoomTypeId(roomTypeId).stream()
                .filter(o -> o.isActiveOn(night))
                .findFirst()
                .map(o -> o.priceOverride().amount())
                .orElseGet(() -> roomTypeRepository.findById(roomTypeId)
                        .map(RoomType::getBasePrice)
                        .map(m -> m.amount())
                        .orElse(BigDecimal.ZERO));
    }
}
