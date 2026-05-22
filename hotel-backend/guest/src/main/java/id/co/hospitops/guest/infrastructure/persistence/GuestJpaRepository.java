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
    Optional<GuestJpaEntity> findByIdNumber(String idNumber);

    boolean existsByIdNumber(String idNumber);

    @Query("""
                SELECT g FROM GuestJpaEntity g
                WHERE LOWER(g.fullName) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR g.idNumber LIKE CONCAT('%', :q, '%')
                ORDER BY g.fullName
            """)
    List<GuestJpaEntity> search(@Param("q") String q, Pageable pageable);

    @Query("""
                SELECT COUNT(g) FROM GuestJpaEntity g
                WHERE LOWER(g.fullName) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR g.idNumber LIKE CONCAT('%', :q, '%')
            """)
    long countByQuery(@Param("q") String q);
}
