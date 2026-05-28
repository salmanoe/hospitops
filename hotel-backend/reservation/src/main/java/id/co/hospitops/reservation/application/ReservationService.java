package id.co.hospitops.reservation.application;

import id.co.hospitops.reservation.application.command.CreateReservationCommand;
import id.co.hospitops.reservation.application.response.ReservationResponse;
import id.co.hospitops.reservation.domain.model.*;
import id.co.hospitops.reservation.domain.port.in.ReservationUseCase;
import id.co.hospitops.reservation.domain.port.out.*;
import id.co.hospitops.shared.*;
import id.co.hospitops.shared.event.*;
import id.co.hospitops.shared.exception.*;
import id.co.hospitops.shared.web.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ReservationService implements ReservationUseCase {

    private final ReservationRepository reservationRepo;
    private final RoomAvailabilityPort roomAvailability;
    private final GuestValidationPort guestValidation;
    private final ReservationNumberGenerator numberGenerator;
    private final ApplicationEventPublisher eventPublisher;
    private final DisplayEnrichmentPort displayEnrichment;

    @Override
    public ReservationResponse create(CreateReservationCommand cmd) {
        if (!guestValidation.exists(cmd.guestId()))
            throw new ResourceNotFoundException("Guest", cmd.guestId().value());

        if (!roomAvailability.isAvailable(cmd.roomId(), cmd.checkIn(), cmd.checkOut()))
            throw new BusinessRuleViolationException("Room is not available for selected dates");

        Money rate = roomAvailability.resolveRate(cmd.roomId(), cmd.checkIn());
        String number = numberGenerator.generate();

        Reservation reservation = Reservation.create(number, cmd.guestId(), cmd.roomId(),
                cmd.checkIn(), cmd.checkOut(), rate, cmd.adults(), cmd.children(),
                cmd.specialRequests(), cmd.createdBy());

        // R-03 FIX: The GIST exclusion constraint (V8 migration) is the authoritative
        // guard against concurrent double-bookings. If two requests race past the
        // availability check above, the DB will reject the second insert. We translate
        // the constraint violation into a business-level ConflictException so the API
        // returns HTTP 409 rather than an unhandled 500.
        Reservation saved;
        try {
            saved = reservationRepo.save(reservation);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Room is no longer available for the selected dates");
        }

        eventPublisher.publishEvent(new ReservationCreatedEvent(
                saved.getId(), saved.getRoomId(), saved.getGuestId(),
                saved.getCheckInDate(), saved.getCheckOutDate()));

        log.info("Reservation {} created for guest {} in room {}",
                saved.getReservationNumber(), saved.getGuestId(), saved.getRoomId());

        return enrich(saved);
    }

    @Override
    public ReservationResponse checkIn(ReservationId id) {
        Reservation res = findReservation(id);
        res.checkIn();
        Reservation saved = reservationRepo.save(res);
        eventPublisher.publishEvent(new ReservationCheckedInEvent(
                saved.getId(), saved.getRoomId(), saved.getGuestId()));
        log.info("Reservation {} checked in", saved.getReservationNumber());
        return enrich(saved);
    }

    @Override
    public ReservationResponse checkOut(ReservationId id) {
        Reservation res = findReservation(id);
        res.checkOut();
        Reservation saved = reservationRepo.save(res);
        eventPublisher.publishEvent(new ReservationCheckedOutEvent(
                saved.getId(), saved.getRoomId(), saved.getGuestId(),
                saved.getNights()));
        log.info("Reservation {} checked out", saved.getReservationNumber());
        return enrich(saved);
    }

    @Override
    public ReservationResponse cancel(ReservationId id) {
        Reservation res = findReservation(id);
        res.cancel();
        Reservation saved = reservationRepo.save(res);
        eventPublisher.publishEvent(new ReservationCancelledEvent(
                saved.getId(), saved.getRoomId()));
        return enrich(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationResponse findById(ReservationId id) {
        return enrich(findReservation(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ReservationResponse> findAll(String statusFilter, Pageable pageable) {
        // W-12 FIX: parse status safely before querying; unknown values return HTTP 400 at controller
        if (statusFilter != null && !statusFilter.isBlank()) {
            ReservationStatus status;
            try {
                status = ReservationStatus.valueOf(statusFilter.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessRuleViolationException(
                        "Unknown reservation status: " + statusFilter);
            }
            List<ReservationResponse> filtered = reservationRepo.findByStatus(status, pageable)
                    .stream().map(this::enrich).toList();
            return PageResult.of(filtered, pageable.getPageNumber(),
                    pageable.getPageSize(), reservationRepo.count());
        }

        // C-2 FIX: use findAll() — not findByGuestId(null) which returned nothing
        List<ReservationResponse> all = reservationRepo.findAll(pageable)
                .stream().map(this::enrich).toList();
        return PageResult.of(all, pageable.getPageNumber(),
                pageable.getPageSize(), reservationRepo.count());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ReservationResponse> findByGuest(GuestId guestId, Pageable pageable) {
        // W-6 FIX: use real count instead of hardcoded 0
        List<ReservationResponse> list = reservationRepo.findByGuestId(guestId, pageable)
                .stream().map(this::enrich).toList();
        long total = reservationRepo.countByGuestId(guestId);
        return PageResult.of(list, pageable.getPageNumber(), pageable.getPageSize(), total);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> todayArrivals() {
        return reservationRepo.findTodayArrivals(LocalDate.now())
                .stream().map(this::enrich).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> todayDepartures() {
        return reservationRepo.findTodayDepartures(LocalDate.now())
                .stream().map(this::enrich).toList();
    }

    private Reservation findReservation(ReservationId id) {
        return reservationRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", id.value()));
    }

    /** Builds an enriched response by resolving display names from guest and room modules. */
    private ReservationResponse enrich(Reservation r) {
        String guestFullName = displayEnrichment.findGuestDisplay(r.getGuestId()).fullName();
        String roomNumber = displayEnrichment.findRoomDisplay(r.getRoomId()).roomNumber();
        return ReservationResponse.from(r, guestFullName, roomNumber);
    }
}
