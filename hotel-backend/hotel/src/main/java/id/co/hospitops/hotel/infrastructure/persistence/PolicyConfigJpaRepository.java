package id.co.hospitops.hotel.infrastructure.persistence;

import id.co.hospitops.hotel.infrastructure.persistence.entity.PolicyConfigJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PolicyConfigJpaRepository extends JpaRepository<PolicyConfigJpaEntity, UUID> {
    Optional<PolicyConfigJpaEntity> findByHotelId(UUID hotelId);
    boolean existsByHotelId(UUID hotelId);
}
