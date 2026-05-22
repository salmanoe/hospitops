package id.co.hospitops.housekeeping.infrastructure.persistence;

import id.co.hospitops.housekeeping.infrastructure.persistence.entity.HousekeepingTaskJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.*;

public interface HousekeepingTaskJpaRepository extends JpaRepository<HousekeepingTaskJpaEntity, UUID> {
    @Query("SELECT t FROM HousekeepingTaskJpaEntity t WHERE t.completed = false ORDER BY t.createdAt DESC")
    List<HousekeepingTaskJpaEntity> findPending(Pageable pageable);

    long countByCompletedFalse();
}
