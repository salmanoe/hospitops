package id.co.hospitops.channel.infrastructure.persistence;

import id.co.hospitops.channel.infrastructure.persistence.entity.ChannelRoomTypeMappingJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChannelRoomTypeMappingJpaRepository
        extends JpaRepository<ChannelRoomTypeMappingJpaEntity, UUID> {

    Optional<ChannelRoomTypeMappingJpaEntity> findByHotelIdAndRoomTypeId(UUID hotelId, UUID roomTypeId);

    List<ChannelRoomTypeMappingJpaEntity> findByHotelId(UUID hotelId);

    boolean existsByHotelIdAndRoomTypeId(UUID hotelId, UUID roomTypeId);
}
