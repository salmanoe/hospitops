package id.co.hospitops.hotel.infrastructure.persistence;

import id.co.hospitops.hotel.infrastructure.persistence.entity.HotelSummaryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

interface HotelSummaryJpaRepository extends JpaRepository<HotelSummaryJpaEntity, UUID> {

    @Query(value = "SELECT s.* FROM hotel_summary s JOIN hotel h ON h.id = s.hotel_id WHERE h.group_id = :groupId",
            nativeQuery = true)
    List<HotelSummaryJpaEntity> findByGroupId(UUID groupId);
}
