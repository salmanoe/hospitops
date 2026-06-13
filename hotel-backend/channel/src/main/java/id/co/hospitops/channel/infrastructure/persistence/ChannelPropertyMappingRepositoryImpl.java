package id.co.hospitops.channel.infrastructure.persistence;

import id.co.hospitops.channel.domain.model.ChannelProvider;
import id.co.hospitops.channel.domain.model.ChannelPropertyMapping;
import id.co.hospitops.channel.domain.port.out.ChannelPropertyMappingRepository;
import id.co.hospitops.channel.infrastructure.persistence.entity.ChannelPropertyMappingJpaEntity;
import id.co.hospitops.shared.ChannelPropertyMappingId;
import id.co.hospitops.shared.HotelContext;
import id.co.hospitops.shared.HotelId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ChannelPropertyMappingRepositoryImpl implements ChannelPropertyMappingRepository {

    private final ChannelPropertyMappingJpaRepository jpa;

    @Override
    public ChannelPropertyMapping save(ChannelPropertyMapping mapping) {
        // Copy onto the already-managed entity when it exists, rather than
        // persisting a fresh instance with the same id — the latter collides
        // in the persistence context (NonUniqueObjectException) when the
        // service loaded the row earlier in the same transaction, and it would
        // also clobber the optimistic-lock version.
        ChannelPropertyMappingJpaEntity entity = jpa.findById(mapping.getId().value())
                .map(existing -> {
                    existing.setProvider(mapping.getProvider());
                    existing.setExternalPropertyId(mapping.getExternalPropertyId());
                    existing.setEnabled(mapping.isEnabled());
                    return existing;
                })
                .orElseGet(() -> toJpa(mapping));
        return toDomain(jpa.save(entity));
    }

    @Override
    public Optional<ChannelPropertyMapping> findByProvider(ChannelProvider provider) {
        return jpa.findByHotelIdAndProvider(HotelContext.current().value(), provider)
                .map(this::toDomain);
    }

    @Override
    public boolean existsByProvider(ChannelProvider provider) {
        return jpa.existsByHotelIdAndProvider(HotelContext.current().value(), provider);
    }

    @Override
    public Optional<ChannelPropertyMapping> findByExternalProperty(ChannelProvider provider, String externalPropertyId) {
        // Global (no hotel scope) — resolves the owning hotel for inbound bookings.
        return jpa.findByProviderAndExternalPropertyId(provider, externalPropertyId)
                .map(this::toDomain);
    }

    // ── Mapping ─────────────────────────────────────────────────────────

    private ChannelPropertyMappingJpaEntity toJpa(ChannelPropertyMapping m) {
        return ChannelPropertyMappingJpaEntity.builder()
                .id(m.getId().value())
                .hotelId(m.getHotelId().value())
                .provider(m.getProvider())
                .externalPropertyId(m.getExternalPropertyId())
                .enabled(m.isEnabled())
                .build();
    }

    private ChannelPropertyMapping toDomain(ChannelPropertyMappingJpaEntity e) {
        return ChannelPropertyMapping.reconstitute(
                ChannelPropertyMappingId.of(e.getId()),
                HotelId.of(e.getHotelId()),
                e.getProvider(),
                e.getExternalPropertyId(),
                e.isEnabled(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
