package id.co.hospitops.room.infrastructure.spi;

import id.co.hospitops.room.domain.model.RoomRateOverride;
import id.co.hospitops.room.domain.model.RoomType;
import id.co.hospitops.room.domain.port.out.RoomRateOverrideRepository;
import id.co.hospitops.room.domain.port.out.RoomRepository;
import id.co.hospitops.room.domain.port.out.RoomTypeRepository;
import id.co.hospitops.room.infrastructure.persistence.RoomJpaRepository;
import id.co.hospitops.shared.Money;
import id.co.hospitops.shared.RoomId;
import id.co.hospitops.shared.RoomTypeId;
import id.co.hospitops.shared.channel.RoomAvailabilitySnapshotProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
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
    public Money ratePerNight(RoomTypeId roomTypeId, LocalDate night) {
        // Prefer the most specific active override (narrowest date span), so a
        // single-day rate set from the calendar wins over a broad seasonal one.
        return overrideRepository.findByRoomTypeId(roomTypeId).stream()
                .filter(o -> o.isActiveOn(night))
                .min(Comparator.comparingLong(o -> o.validUntil().toEpochDay() - o.validFrom().toEpochDay()))
                .map(RoomRateOverride::priceOverride)
                .orElseGet(() -> roomTypeRepository.findById(roomTypeId)
                        .map(RoomType::getBasePrice)
                        .orElse(Money.zero()));
    }
}
