package id.co.hospitops.identity.infrastructure.persistence;

import id.co.hospitops.identity.infrastructure.persistence.entity.StaffJpaEntity;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StaffJpaRepository extends JpaRepository<StaffJpaEntity, UUID> {
    // Auth path — must NOT filter by hotel_id (HotelContext is unbound during authentication)
    Optional<StaffJpaEntity> findByUsername(String username);

    // Hotel-scoped lookup — use for management operations where HotelContext is bound
    Optional<StaffJpaEntity> findByIdAndHotelId(UUID id, UUID hotelId);

    // Global uniqueness check — username is unique across all hotels (DB constraint),
    // which is what lets staff log in with username + password alone.
    boolean existsByUsername(String username);

    // Hotel-scoped list (used for staff management endpoints)
    @NonNull Page<StaffJpaEntity> findByHotelId(@NonNull UUID hotelId, @NonNull Pageable pageable);

    long countByHotelId(UUID hotelId);
}
