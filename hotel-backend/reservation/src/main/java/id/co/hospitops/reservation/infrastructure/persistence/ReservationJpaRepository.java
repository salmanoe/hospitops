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
    Optional<ReservationJpaEntity> findByIdAndHotelId(UUID id, UUID hotelId);

    Optional<ReservationJpaEntity> findByReservationNumberAndHotelId(String number, UUID hotelId);

    // Hotel-scoped list queries
    List<ReservationJpaEntity> findByHotelId(UUID hotelId, Pageable pageable);

    List<ReservationJpaEntity> findByHotelIdAndGuestId(UUID hotelId, UUID guestId, Pageable pageable);

    long countByHotelIdAndGuestId(UUID hotelId, UUID guestId);

    List<ReservationJpaEntity> findByHotelIdAndStatus(UUID hotelId, ReservationStatus status,
                                                      Pageable pageable);

    long countByHotelId(UUID hotelId);

    @Query("""
                SELECT r FROM ReservationJpaEntity r
                WHERE r.hotelId = :hotelId
                  AND r.checkInDate = :date
                  AND r.status = :status
                ORDER BY r.reservationNumber
            """)
    List<ReservationJpaEntity> findTodayArrivals(@Param("hotelId") UUID hotelId,
                                                 @Param("date") LocalDate date,
                                                 @Param("status") ReservationStatus status);

    @Query("""
                SELECT r FROM ReservationJpaEntity r
                WHERE r.hotelId = :hotelId
                  AND r.checkOutDate = :date
                  AND r.status = :status
                ORDER BY r.reservationNumber
            """)
    List<ReservationJpaEntity> findTodayDepartures(@Param("hotelId") UUID hotelId,
                                                   @Param("date") LocalDate date,
                                                   @Param("status") ReservationStatus status);

    @Query("""
                SELECT r FROM ReservationJpaEntity r
                WHERE r.hotelId = :hotelId
                  AND r.checkInDate < :to
                  AND r.checkOutDate > :from
                ORDER BY r.roomId, r.checkInDate
            """)
    List<ReservationJpaEntity> findOverlapping(@Param("hotelId") UUID hotelId,
                                               @Param("from") LocalDate from,
                                               @Param("to") LocalDate to);
}
