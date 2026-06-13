package id.co.hospitops.housekeeping.infrastructure.persistence;

import id.co.hospitops.housekeeping.domain.model.HousekeepingTask;
import id.co.hospitops.housekeeping.domain.port.out.HousekeepingTaskRepository;
import id.co.hospitops.shared.HotelContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@RequiredArgsConstructor
public class HousekeepingTaskRepositoryImpl implements HousekeepingTaskRepository {
    private final HousekeepingTaskJpaRepository jpa;
    private final HousekeepingTaskMapper mapper;

    @Override
    public HousekeepingTask save(HousekeepingTask t) {
        return mapper.toDomain(jpa.save(mapper.toJpa(t)));
    }

    @Override
    public Optional<HousekeepingTask> findById(UUID id) {
        return jpa.findByIdAndHotelId(id, HotelContext.current().value())
                .map(mapper::toDomain);
    }

    @Override
    public List<HousekeepingTask> findPending(Pageable p) {
        return jpa.findPendingByHotelId(HotelContext.current().value(), p)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public long countPending() {
        return jpa.countByHotelIdAndCompletedFalse(HotelContext.current().value());
    }
}
