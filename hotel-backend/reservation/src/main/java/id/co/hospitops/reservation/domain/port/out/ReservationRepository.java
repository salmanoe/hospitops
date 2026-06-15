package id.co.hospitops.reservation.domain.port.out;

import id.co.hospitops.reservation.domain.model.Reservation;
import id.co.hospitops.reservation.domain.model.ReservationStatus;
import id.co.hospitops.shared.*;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    Reservation save(Reservation reservation);

    Optional<Reservation> findById(ReservationId id);

    Optional<Reservation> findByReservationNumber(String number);

    List<Reservation> findAll(Pageable pageable);

    List<Reservation> findByGuestId(GuestId guestId, Pageable pageable);

    long countByGuestId(GuestId guestId);

    List<Reservation> findByStatus(ReservationStatus status, Pageable pageable);

    List<Reservation> findTodayArrivals(LocalDate date);

    List<Reservation> findTodayDepartures(LocalDate date);

    /**
     * All reservations whose stay overlaps the window {@code [from, to)}
     * (i.e. checkIn &lt; to AND checkOut &gt; from), ordered by room then
     * check-in. Backs the booking calendar and revenue analytics.
     */
    List<Reservation> findOverlapping(LocalDate from, LocalDate to);

    long count();
}
