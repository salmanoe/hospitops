package id.co.hospitops.room.infrastructure.persistence;

import id.co.hospitops.room.domain.model.Room;
import id.co.hospitops.room.domain.model.RoomStatus;
import id.co.hospitops.room.domain.port.out.RoomRepository;
import id.co.hospitops.room.infrastructure.persistence.entity.RoomJpaEntity;
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
        return jpa.findById(id.value()).map(this::toDomain);
    }

    @Override
    public boolean existsByRoomNumber(String roomNumber) {
        return jpa.existsByRoomNumber(roomNumber);
    }

    @Override
    public List<Room> findAll(Pageable pageable) {
        return jpa.findAll(pageable).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Room> findByStatus(RoomStatus status, Pageable pageable) {
        return jpa.findByStatus(status, pageable).stream().map(this::toDomain).toList();
    }

    @Override
    public long count() {
        return jpa.count();
    }

    @Override
    public long countByStatus(RoomStatus status) {
        return jpa.countByStatus(status);
    }

    @Override
    public List<Room> findAvailable(LocalDate checkIn, LocalDate checkOut) {
        return jpa.findAvailable(checkIn, checkOut).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean isAvailable(RoomId roomId, LocalDate checkIn, LocalDate checkOut) {
        return jpa.existsAvailableRoom(roomId.value(), checkIn, checkOut);
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
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
