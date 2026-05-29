package id.co.hospitops.hotel.infrastructure.persistence;

import id.co.hospitops.hotel.infrastructure.persistence.entity.HotelJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface HotelJpaRepository extends JpaRepository<HotelJpaEntity, UUID> {
    List<HotelJpaEntity> findByGroupId(UUID groupId);
}
