package id.co.hospitops.room.infrastructure.persistence;

import id.co.hospitops.room.domain.model.RoomType;
import id.co.hospitops.room.domain.port.out.RoomTypeRepository;
import id.co.hospitops.room.infrastructure.persistence.entity.RoomTypeJpaEntity;
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
        return jpa.findById(id.value()).map(this::toDomain);
    }

    @Override
    public boolean existsByName(String name) {
        return jpa.existsByName(name);
    }

    @Override
    public List<RoomType> findAll(Pageable pageable) {
        return jpa.findAll(pageable).stream().map(this::toDomain).toList();
    }

    @Override
    public long count() {
        return jpa.count();
    }

    // ── Mapping ─────────────────────────────────────────────────────────

    private RoomTypeJpaEntity toJpa(RoomType rt) {
        return RoomTypeJpaEntity.builder()
                .id(rt.getId().value())
                .name(rt.getName())
                .capacity(rt.getCapacity())
                .description(rt.getDescription())
                .basePrice(rt.getBasePrice().amount())
                .build();
    }

    private RoomType toDomain(RoomTypeJpaEntity e) {
        return RoomType.reconstitute(
                RoomTypeId.of(e.getId()),
                e.getName(),
                e.getCapacity(),
                e.getDescription(),
                new Money(e.getBasePrice(), Currency.getInstance("IDR")),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
