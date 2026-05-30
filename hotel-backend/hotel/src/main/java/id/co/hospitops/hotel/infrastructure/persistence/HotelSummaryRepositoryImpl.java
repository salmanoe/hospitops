package id.co.hospitops.hotel.infrastructure.persistence;

import id.co.hospitops.hotel.domain.model.HotelSummary;
import id.co.hospitops.hotel.domain.port.out.HotelSummaryRepository;
import id.co.hospitops.hotel.infrastructure.persistence.entity.HotelSummaryJpaEntity;
import id.co.hospitops.shared.GroupId;
import id.co.hospitops.shared.HotelId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
class HotelSummaryRepositoryImpl implements HotelSummaryRepository {

    private final HotelSummaryJpaRepository jpa;

    @Override
    public HotelSummary save(HotelSummary summary) {
        HotelSummaryJpaEntity saved = jpa.save(toJpa(summary));
        return toDomain(saved);
    }

    @Override
    public Optional<HotelSummary> findByHotelId(HotelId hotelId) {
        return jpa.findById(hotelId.value()).map(this::toDomain);
    }

    @Override
    public List<HotelSummary> findByGroupId(GroupId groupId) {
        return jpa.findByGroupId(groupId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    private HotelSummaryJpaEntity toJpa(HotelSummary s) {
        return HotelSummaryJpaEntity.builder()
                .hotelId(s.getHotelId().value())
                .occupiedRooms(s.getOccupiedRooms())
                .totalRooms(s.getTotalRooms())
                .arrivalsToday(s.getArrivalsToday())
                .departuresToday(s.getDeparturesToday())
                .revenueToday(s.getRevenueToday())
                .revenueMonth(s.getRevenueMonth())
                .dirtyRooms(s.getDirtyRooms())
                .build();
    }

    private HotelSummary toDomain(HotelSummaryJpaEntity e) {
        return HotelSummary.reconstitute(
                HotelId.of(e.getHotelId()),
                e.getOccupiedRooms(), e.getTotalRooms(),
                e.getArrivalsToday(), e.getDeparturesToday(),
                e.getRevenueToday() != null ? e.getRevenueToday() : BigDecimal.ZERO,
                e.getRevenueMonth() != null ? e.getRevenueMonth() : BigDecimal.ZERO,
                e.getDirtyRooms(),
                e.getUpdatedAt());
    }
}
