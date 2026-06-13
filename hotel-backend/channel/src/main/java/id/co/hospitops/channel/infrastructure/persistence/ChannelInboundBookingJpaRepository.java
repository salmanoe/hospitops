package id.co.hospitops.channel.infrastructure.persistence;

import id.co.hospitops.channel.infrastructure.persistence.entity.ChannelInboundBookingJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ChannelInboundBookingJpaRepository
        extends JpaRepository<ChannelInboundBookingJpaEntity, UUID> {

    Optional<ChannelInboundBookingJpaEntity> findByHotelIdAndExternalBookingId(
            UUID hotelId, String externalBookingId);
}
