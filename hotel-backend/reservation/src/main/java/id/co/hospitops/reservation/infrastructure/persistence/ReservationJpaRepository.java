package id.co.hospitops.reservation.infrastructure.persistence;

import id.co.hospitops.reservation.domain.model.ReservationStatus;
import id.co.hospitops.reservation.infrastructure.persistence.entity.ReservationJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationJpaRepository extends JpaRepository<ReservationJpaEntity, UUID> {
    Optional<ReservationJpaEntity> findByReservationNumber(String number);

    List<ReservationJpaEntity> findByGuestId(UUID guestId, Pageable pageable);

    long countByGuestId(UUID guestId);

    List<ReservationJpaEntity> findByStatus(ReservationStatus status, Pageable pageable);

    @Query("""
                SELECT r FROM ReservationJpaEntity r
                WHERE r.checkInDate = :date
                  AND r.status = :status
                ORDER BY r.reservationNumber
            """)
    List<ReservationJpaEntity> findTodayArrivals(@Param("date") LocalDate date,
                                                 @Param("status") ReservationStatus status);

    @Query("""
                SELECT r FROM ReservationJpaEntity r
                WHERE r.checkOutDate = :date
                  AND r.status = :status
                ORDER BY r.reservationNumber
            """)
    List<ReservationJpaEntity> findTodayDepartures(@Param("date") LocalDate date,
                                                   @Param("status") ReservationStatus status);
}
