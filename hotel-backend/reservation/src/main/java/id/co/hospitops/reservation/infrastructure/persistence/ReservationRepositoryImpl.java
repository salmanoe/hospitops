package id.co.hospitops.reservation.infrastructure.persistence;

import id.co.hospitops.reservation.domain.model.Reservation;
import id.co.hospitops.reservation.domain.model.ReservationStatus;
import id.co.hospitops.reservation.domain.port.out.ReservationRepository;
import id.co.hospitops.reservation.infrastructure.persistence.entity.ReservationJpaEntity;
import id.co.hospitops.shared.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReservationRepositoryImpl implements ReservationRepository {

    private final ReservationJpaRepository jpa;

    @Override
    public Reservation save(Reservation r) {
        return toDomain(jpa.save(toJpa(r)));
    }

    @Override
    public Optional<Reservation> findById(ReservationId id) {
        return jpa.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Optional<Reservation> findByReservationNumber(String number) {
        return jpa.findByReservationNumber(number).map(this::toDomain);
    }

    @Override
    public List<Reservation> findAll(Pageable pageable) {
        return jpa.findAll(pageable).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Reservation> findByGuestId(GuestId guestId, Pageable pageable) {
        return jpa.findByGuestId(guestId.value(), pageable).stream().map(this::toDomain).toList();
    }

    @Override
    public long countByGuestId(GuestId guestId) {
        return jpa.countByGuestId(guestId.value());
    }

    @Override
    public List<Reservation> findByStatus(ReservationStatus status, Pageable pageable) {
        return jpa.findByStatus(status, pageable).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Reservation> findTodayArrivals(LocalDate date) {
        return jpa.findTodayArrivals(date, ReservationStatus.CONFIRMED).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Reservation> findTodayDepartures(LocalDate date) {
        return jpa.findTodayDepartures(date, ReservationStatus.CHECKED_IN).stream().map(this::toDomain).toList();
    }

    @Override
    public long count() {
        return jpa.count();
    }

    private ReservationJpaEntity toJpa(Reservation r) {
        return ReservationJpaEntity.builder()
                .id(r.getId().value())
                .reservationNumber(r.getReservationNumber())
                .guestId(r.getGuestId().value())
                .roomId(r.getRoomId().value())
                .createdBy(r.getCreatedBy() != null ? r.getCreatedBy().value() : null)
                .checkInDate(r.getCheckInDate())
                .checkOutDate(r.getCheckOutDate())
                .status(r.getStatus())
                .ratePerNight(r.getRatePerNight().amount())
                .adults(r.getAdults())
                .children(r.getChildren())
                .specialRequests(r.getSpecialRequests())
                .build();
    }

    private Reservation toDomain(ReservationJpaEntity e) {
        return Reservation.reconstitute(
                ReservationId.of(e.getId()),
                e.getReservationNumber(),
                GuestId.of(e.getGuestId()),
                RoomId.of(e.getRoomId()),
                e.getCheckInDate(), e.getCheckOutDate(),
                e.getStatus(),
                new Money(e.getRatePerNight(), Currency.getInstance("IDR")),
                e.getAdults(), e.getChildren(),
                e.getSpecialRequests(),
                e.getCreatedBy() != null ? StaffId.of(e.getCreatedBy()) : null,
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
