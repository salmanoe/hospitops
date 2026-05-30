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

    // Hotel-scoped login — used by HotelAuthService (hotelId comes from path param, not HotelContext)
    Optional<StaffJpaEntity> findByUsernameAndHotelId(String username, UUID hotelId);

    // Hotel-scoped uniqueness check (used when creating a staff member)
    boolean existsByUsernameAndHotelId(String username, UUID hotelId);

    // Hotel-scoped list (used for staff management endpoints)
    @NonNull Page<StaffJpaEntity> findByHotelId(@NonNull UUID hotelId, @NonNull Pageable pageable);

    long countByHotelId(UUID hotelId);
}
