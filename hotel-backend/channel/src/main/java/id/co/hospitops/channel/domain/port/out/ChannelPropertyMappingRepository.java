package id.co.hospitops.channel.domain.port.out;

import id.co.hospitops.channel.domain.model.ChannelProvider;
import id.co.hospitops.channel.domain.model.ChannelPropertyMapping;

import java.util.Optional;

/**
 * Persistence port for {@link ChannelPropertyMapping}. All reads are scoped
 * to the current {@code HotelContext} by the adapter.
 */
public interface ChannelPropertyMappingRepository {

    ChannelPropertyMapping save(ChannelPropertyMapping mapping);

    /** The current hotel's mapping for a provider, if any. */
    Optional<ChannelPropertyMapping> findByProvider(ChannelProvider provider);

    boolean existsByProvider(ChannelProvider provider);

    /**
     * Reverse lookup by the provider's property id, across ALL hotels (no
     * HotelContext) — used by the inbound processor to resolve which hotel an
     * OTA booking belongs to.
     */
    Optional<ChannelPropertyMapping> findByExternalProperty(ChannelProvider provider, String externalPropertyId);
}
