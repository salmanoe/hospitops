package id.co.hospitops.reservation.domain.port.in;

import id.co.hospitops.reservation.application.command.CreateReservationCommand;
import id.co.hospitops.reservation.application.response.ReservationResponse;
import id.co.hospitops.reservation.application.response.RevenueMetricsResponse;
import id.co.hospitops.shared.*;
import id.co.hospitops.shared.web.PageResult;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface ReservationUseCase {
    ReservationResponse create(CreateReservationCommand command);

    ReservationResponse checkIn(ReservationId id);

    ReservationResponse checkOut(ReservationId id);

    ReservationResponse cancel(ReservationId id);

    ReservationResponse findById(ReservationId id);

    PageResult<ReservationResponse> findAll(String statusFilter, Pageable pageable);

    PageResult<ReservationResponse> findByGuest(GuestId guestId, Pageable pageable);

    List<ReservationResponse> todayArrivals();

    List<ReservationResponse> todayDepartures();

    /** Reservations whose stay overlaps {@code [from, to)} — backs the booking calendar. */
    List<ReservationResponse> findInRange(LocalDate from, LocalDate to);

    /** ADR / RevPAR / occupancy for {@code [from, to)} — backs the analytics dashboard. */
    RevenueMetricsResponse revenueMetrics(LocalDate from, LocalDate to);
}
