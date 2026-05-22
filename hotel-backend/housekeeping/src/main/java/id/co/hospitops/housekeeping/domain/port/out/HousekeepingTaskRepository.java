package id.co.hospitops.housekeeping.domain.port.out;

import id.co.hospitops.housekeeping.domain.model.HousekeepingTask;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HousekeepingTaskRepository {
    HousekeepingTask save(HousekeepingTask task);

    Optional<HousekeepingTask> findById(UUID id);

    List<HousekeepingTask> findPending(Pageable pageable);

    long countPending();
}
