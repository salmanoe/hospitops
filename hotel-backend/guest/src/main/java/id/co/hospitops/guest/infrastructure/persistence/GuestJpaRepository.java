package id.co.hospitops.guest.infrastructure.persistence;

import id.co.hospitops.guest.infrastructure.persistence.entity.GuestJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GuestJpaRepository extends JpaRepository<GuestJpaEntity, UUID> {
    // Hotel-scoped lookups
    Optional<GuestJpaEntity> findByIdAndHotelId(UUID id, UUID hotelId);

    Optional<GuestJpaEntity> findByIdNumberAndHotelId(String idNumber, UUID hotelId);

    boolean existsByIdNumberAndHotelId(String idNumber, UUID hotelId);

    long countByHotelId(UUID hotelId);

    @Query("""
                SELECT g FROM GuestJpaEntity g
                WHERE g.hotelId = :hotelId
                  AND (LOWER(g.fullName) LIKE LOWER(CONCAT('%', :q, '%'))
                       OR g.idNumber LIKE CONCAT('%', :q, '%'))
                ORDER BY g.fullName
            """)
    List<GuestJpaEntity> searchByHotelId(@Param("hotelId") UUID hotelId,
                                         @Param("q") String q, Pageable pageable);

    @Query("""
                SELECT COUNT(g) FROM GuestJpaEntity g
                WHERE g.hotelId = :hotelId
                  AND (LOWER(g.fullName) LIKE LOWER(CONCAT('%', :q, '%'))
                       OR g.idNumber LIKE CONCAT('%', :q, '%'))
            """)
    long countByHotelIdAndQuery(@Param("hotelId") UUID hotelId, @Param("q") String q);

    List<GuestJpaEntity> findByHotelId(UUID hotelId, Pageable pageable);
}
