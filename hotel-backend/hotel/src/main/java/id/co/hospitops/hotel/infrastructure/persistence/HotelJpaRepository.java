package id.co.hospitops.hotel.infrastructure.persistence;

import id.co.hospitops.hotel.domain.model.HotelStatus;
import id.co.hospitops.hotel.infrastructure.persistence.entity.HotelJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface HotelJpaRepository extends JpaRepository<HotelJpaEntity, UUID> {

    List<HotelJpaEntity> findByGroupId(UUID groupId);

    /**
     * Returns only the {@code status} scalar for a hotel.
     * Used by {@code HotelStatusPortAdapter} on every staff login — avoids loading
     * the full entity + checklist join just to check ACTIVE/SETUP/SUSPENDED.
     */
    @Query("SELECT h.status FROM HotelJpaEntity h WHERE h.id = :id")
    Optional<HotelStatus> findStatusById(UUID id);

    /**
     * Returns a closed projection carrying {@code status} and {@code groupId} together.
     * Used by {@code HotelLookupPortAdapter} for the {@code /enter} endpoint which
     * must verify both fields in a single DB round-trip.
     *
     * <p>Spring Data JPA closed projections are supported for JPQL multi-column results;
     * {@code Optional<Object[]>} is NOT supported and causes a context startup failure.
     */
    @Query("SELECT h.status AS status, h.groupId AS groupId, h.name AS name FROM HotelJpaEntity h WHERE h.id = :id")
    Optional<HotelStatusView> findStatusAndGroupById(UUID id);

    /** Closed projection — status + groupId + name, no entity hydration. */
    interface HotelStatusView {
        HotelStatus getStatus();
        UUID getGroupId();
        String getName();
    }
}
