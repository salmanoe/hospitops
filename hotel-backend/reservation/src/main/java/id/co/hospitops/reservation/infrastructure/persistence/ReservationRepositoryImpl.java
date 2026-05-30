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
        return jpa.findByIdAndHotelId(id.value(), HotelContext.current().value())
                .map(this::toDomain);
    }

    @Override
    public Optional<Reservation> findByReservationNumber(String number) {
        return jpa.findByReservationNumberAndHotelId(number, HotelContext.current().value())
                .map(this::toDomain);
    }

    @Override
    public List<Reservation> findAll(Pageable pageable) {
        return jpa.findByHotelId(HotelContext.current().value(), pageable)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<Reservation> findByGuestId(GuestId guestId, Pageable pageable) {
        return jpa.findByHotelIdAndGuestId(HotelContext.current().value(),
                guestId.value(), pageable).stream().map(this::toDomain).toList();
    }

    @Override
    public long countByGuestId(GuestId guestId) {
        return jpa.countByHotelIdAndGuestId(HotelContext.current().value(), guestId.value());
    }

    @Override
    public List<Reservation> findByStatus(ReservationStatus status, Pageable pageable) {
        return jpa.findByHotelIdAndStatus(HotelContext.current().value(), status, pageable)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<Reservation> findTodayArrivals(LocalDate date) {
        return jpa.findTodayArrivals(HotelContext.current().value(), date,
                ReservationStatus.CONFIRMED).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Reservation> findTodayDepartures(LocalDate date) {
        return jpa.findTodayDepartures(HotelContext.current().value(), date,
                ReservationStatus.CHECKED_IN).stream().map(this::toDomain).toList();
    }

    @Override
    public long count() {
        return jpa.countByHotelId(HotelContext.current().value());
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
                .hotelId(r.getHotelId().value())
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
                HotelId.of(e.getHotelId()),
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
