package id.co.hospitops.identity.domain.port.out;

import id.co.hospitops.identity.domain.model.Staff;
import id.co.hospitops.shared.StaffId;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface StaffRepository {
    Staff save(Staff staff);

    Optional<Staff> findById(StaffId id);

    Optional<Staff> findByUsername(String username);

    boolean existsByUsername(String username);

    List<Staff> findAll(Pageable pageable);

    long count();
}
