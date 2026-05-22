package id.co.hospitops.identity.infrastructure.persistence;

import id.co.hospitops.identity.domain.model.Staff;
import id.co.hospitops.identity.domain.port.out.StaffRepository;
import id.co.hospitops.shared.StaffId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StaffRepositoryImpl implements StaffRepository {

    private final StaffJpaRepository jpaRepo;
    private final StaffMapper mapper;

    @Override
    public Staff save(Staff staff) {
        return mapper.toDomain(jpaRepo.save(mapper.toJpa(staff)));
    }

    @Override
    public Optional<Staff> findById(StaffId id) {
        return jpaRepo.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<Staff> findByUsername(String u) {
        return jpaRepo.findByUsername(u).map(mapper::toDomain);
    }

    @Override
    public boolean existsByUsername(String u) {
        return jpaRepo.existsByUsername(u);
    }

    @Override
    public List<Staff> findAll(Pageable pageable) {
        return jpaRepo.findAll(pageable).map(mapper::toDomain).getContent();
    }

    @Override
    public long count() {
        return jpaRepo.count();
    }
}
