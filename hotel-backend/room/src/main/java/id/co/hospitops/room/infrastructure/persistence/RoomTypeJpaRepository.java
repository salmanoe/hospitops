package id.co.hospitops.room.infrastructure.persistence;

import id.co.hospitops.room.infrastructure.persistence.entity.RoomTypeJpaEntity;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RoomTypeJpaRepository extends JpaRepository<RoomTypeJpaEntity, UUID> {

    boolean existsByName(String name);

    @NonNull Page<RoomTypeJpaEntity> findAll(@NonNull Pageable pageable);
}
