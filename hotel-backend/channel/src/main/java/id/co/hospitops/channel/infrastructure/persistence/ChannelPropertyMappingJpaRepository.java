package id.co.hospitops.channel.infrastructure.persistence;

import id.co.hospitops.channel.domain.model.ChannelProvider;
import id.co.hospitops.channel.infrastructure.persistence.entity.ChannelPropertyMappingJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ChannelPropertyMappingJpaRepository
        extends JpaRepository<ChannelPropertyMappingJpaEntity, UUID> {

    Optional<ChannelPropertyMappingJpaEntity> findByHotelIdAndProvider(UUID hotelId, ChannelProvider provider);

    boolean existsByHotelIdAndProvider(UUID hotelId, ChannelProvider provider);
}
