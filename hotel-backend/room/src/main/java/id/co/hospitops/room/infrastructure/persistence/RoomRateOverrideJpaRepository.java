package id.co.hospitops.room.infrastructure.persistence;

import id.co.hospitops.room.infrastructure.persistence.entity.RoomRateOverrideJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface RoomRateOverrideJpaRepository extends JpaRepository<RoomRateOverrideJpaEntity, UUID> {
    List<RoomRateOverrideJpaEntity> findByRoomTypeId(UUID roomTypeId);

    @Query("""
                SELECT o FROM RoomRateOverrideJpaEntity o
                WHERE o.roomTypeId = :roomTypeId
                  AND o.validFrom <= :date
                  AND o.validUntil >= :date
            """)
    List<RoomRateOverrideJpaEntity> findActiveOn(
            @Param("roomTypeId") UUID roomTypeId,
            @Param("date") LocalDate date
    );
}
