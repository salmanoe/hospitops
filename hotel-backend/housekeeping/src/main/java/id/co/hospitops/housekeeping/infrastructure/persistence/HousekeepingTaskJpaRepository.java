package id.co.hospitops.housekeeping.infrastructure.persistence;

import id.co.hospitops.housekeeping.infrastructure.persistence.entity.HousekeepingTaskJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface HousekeepingTaskJpaRepository extends JpaRepository<HousekeepingTaskJpaEntity, UUID> {

    Optional<HousekeepingTaskJpaEntity> findByIdAndHotelId(UUID id, UUID hotelId);

    @Query("SELECT t FROM HousekeepingTaskJpaEntity t WHERE t.hotelId = :hotelId AND t.completed = false ORDER BY t.createdAt DESC")
    List<HousekeepingTaskJpaEntity> findPendingByHotelId(@Param("hotelId") UUID hotelId, Pageable pageable);

    long countByHotelIdAndCompletedFalse(UUID hotelId);
}
