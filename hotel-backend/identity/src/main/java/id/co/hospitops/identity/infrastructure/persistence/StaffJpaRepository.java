package id.co.hospitops.identity.infrastructure.persistence;

import id.co.hospitops.identity.infrastructure.persistence.entity.StaffJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StaffJpaRepository extends JpaRepository<StaffJpaEntity, UUID> {
    Optional<StaffJpaEntity> findByUsername(String username);

    boolean existsByUsername(String username);

    Page<StaffJpaEntity> findAll(Pageable pageable);
}
