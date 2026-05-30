package id.co.hospitops.room.infrastructure.persistence;

import id.co.hospitops.room.infrastructure.persistence.entity.RoomTypeJpaEntity;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoomTypeJpaRepository extends JpaRepository<RoomTypeJpaEntity, UUID> {

    Optional<RoomTypeJpaEntity> findByIdAndHotelId(UUID id, UUID hotelId);

    boolean existsByNameAndHotelId(String name, UUID hotelId);

    @NonNull Page<RoomTypeJpaEntity> findByHotelId(@NonNull UUID hotelId, @NonNull Pageable pageable);

    long countByHotelId(UUID hotelId);
}
