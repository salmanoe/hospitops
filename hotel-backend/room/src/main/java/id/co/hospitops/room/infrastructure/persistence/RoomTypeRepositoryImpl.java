package id.co.hospitops.room.infrastructure.persistence;

import id.co.hospitops.room.domain.model.RoomType;
import id.co.hospitops.room.domain.port.out.RoomTypeRepository;
import id.co.hospitops.room.infrastructure.persistence.entity.RoomTypeJpaEntity;
import id.co.hospitops.shared.HotelContext;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.Money;
import id.co.hospitops.shared.RoomTypeId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Currency;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RoomTypeRepositoryImpl implements RoomTypeRepository {

    private final RoomTypeJpaRepository jpa;

    @Override
    public RoomType save(RoomType rt) {
        return toDomain(jpa.save(toJpa(rt)));
    }

    @Override
    public Optional<RoomType> findById(RoomTypeId id) {
        return jpa.findByIdAndHotelId(id.value(), HotelContext.current().value())
                .map(this::toDomain);
    }

    @Override
    public boolean existsByName(String name) {
        return jpa.existsByNameAndHotelId(name, HotelContext.current().value());
    }

    @Override
    public List<RoomType> findAll(Pageable pageable) {
        return jpa.findByHotelId(HotelContext.current().value(), pageable)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public long count() {
        return jpa.countByHotelId(HotelContext.current().value());
    }

    // ── Mapping ─────────────────────────────────────────────────────────

    private RoomTypeJpaEntity toJpa(RoomType rt) {
        return RoomTypeJpaEntity.builder()
                .id(rt.getId().value())
                .name(rt.getName())
                .capacity(rt.getCapacity())
                .description(rt.getDescription())
                .basePrice(rt.getBasePrice().amount())
                .hotelId(rt.getHotelId().value())
                .build();
    }

    private RoomType toDomain(RoomTypeJpaEntity e) {
        return RoomType.reconstitute(
                RoomTypeId.of(e.getId()),
                e.getName(),
                e.getCapacity(),
                e.getDescription(),
                new Money(e.getBasePrice(), Currency.getInstance("IDR")),
                HotelId.of(e.getHotelId()),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
