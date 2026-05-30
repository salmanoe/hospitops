package id.co.hospitops.room.infrastructure.persistence;

import id.co.hospitops.room.domain.model.Room;
import id.co.hospitops.room.domain.model.RoomStatus;
import id.co.hospitops.room.domain.port.out.RoomRepository;
import id.co.hospitops.room.infrastructure.persistence.entity.RoomJpaEntity;
import id.co.hospitops.shared.HotelContext;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.RoomId;
import id.co.hospitops.shared.RoomTypeId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RoomRepositoryImpl implements RoomRepository {

    private final RoomJpaRepository jpa;

    @Override
    public Room save(Room room) {
        return toDomain(jpa.save(toJpa(room)));
    }

    @Override
    public Optional<Room> findById(RoomId id) {
        return jpa.findByIdAndHotelId(id.value(), HotelContext.current().value())
                .map(this::toDomain);
    }

    @Override
    public boolean existsByRoomNumber(String roomNumber) {
        return jpa.existsByRoomNumberAndHotelId(roomNumber, HotelContext.current().value());
    }

    @Override
    public List<Room> findAll(Pageable pageable) {
        return jpa.findByHotelId(HotelContext.current().value(), pageable)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<Room> findByStatus(RoomStatus status, Pageable pageable) {
        return jpa.findByHotelIdAndStatus(HotelContext.current().value(), status, pageable)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public long count() {
        return jpa.countByHotelId(HotelContext.current().value());
    }

    @Override
    public long countByStatus(RoomStatus status) {
        return jpa.countByHotelIdAndStatus(HotelContext.current().value(), status);
    }

    @Override
    public List<Room> findAvailable(LocalDate checkIn, LocalDate checkOut) {
        return jpa.findAvailable(HotelContext.current().value(), checkIn, checkOut)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public boolean isAvailable(RoomId roomId, LocalDate checkIn, LocalDate checkOut) {
        return jpa.existsAvailableRoom(roomId.value(), HotelContext.current().value(),
                checkIn, checkOut);
    }

    // ── Mapping ─────────────────────────────────────────────────────────

    private RoomJpaEntity toJpa(Room r) {
        return RoomJpaEntity.builder()
                .id(r.getId().value())
                .roomNumber(r.getRoomNumber())
                .floor(r.getFloor())
                .status(r.getStatus())
                .roomTypeId(r.getRoomTypeId().value())
                .notes(r.getNotes())
                .hotelId(r.getHotelId().value())
                .build();
    }

    private Room toDomain(RoomJpaEntity e) {
        return Room.reconstitute(
                RoomId.of(e.getId()),
                e.getRoomNumber(),
                e.getFloor(),
                e.getStatus(),
                RoomTypeId.of(e.getRoomTypeId()),
                e.getNotes(),
                HotelId.of(e.getHotelId()),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
