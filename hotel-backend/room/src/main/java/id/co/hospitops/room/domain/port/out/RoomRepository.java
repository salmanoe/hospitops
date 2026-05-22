package id.co.hospitops.room.domain.port.out;

import id.co.hospitops.room.domain.model.Room;
import id.co.hospitops.room.domain.model.RoomStatus;
import id.co.hospitops.shared.RoomId;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RoomRepository {

    Room save(Room room);

    Optional<Room> findById(RoomId id);

    boolean existsByRoomNumber(String roomNumber);

    List<Room> findAll(Pageable pageable);

    List<Room> findByStatus(RoomStatus status, Pageable pageable);

    long count();

    long countByStatus(RoomStatus status);

    /**
     * Returns all AVAILABLE rooms that have no overlapping CONFIRMED or
     * CHECKED_IN reservation in [checkIn, checkOut). Uses a NOT EXISTS SQL
     * subquery — O(1) regardless of total room count.
     */
    List<Room> findAvailable(LocalDate checkIn, LocalDate checkOut);

    /**
     * Returns {@code true} if the specific room is available for the given
     * date range. Backed by a single-row EXISTS query — no Java-side scan.
     */
    boolean isAvailable(RoomId roomId, LocalDate checkIn, LocalDate checkOut);
}
