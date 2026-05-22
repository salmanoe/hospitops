package id.co.hospitops.room.infrastructure.persistence;

import id.co.hospitops.room.domain.model.RoomStatus;
import id.co.hospitops.room.infrastructure.persistence.entity.RoomJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomJpaRepository extends JpaRepository<RoomJpaEntity, UUID> {

    Optional<RoomJpaEntity> findByRoomNumber(String roomNumber);

    boolean existsByRoomNumber(String roomNumber);

    List<RoomJpaEntity> findByStatus(RoomStatus status, Pageable pageable);

    long countByStatus(RoomStatus status);

    /**
     * Returns all AVAILABLE rooms with no overlapping active reservation.
     *
     * <p><strong>R3-04 NOTE — Cross-module SQL dependency:</strong>
     * This query references the {@code reservation} table, which belongs to the
     * reservation module. This is a deliberate Stage 1/2 monolith compromise:
     * introducing a proper {@code OccupancyPort} in the shared kernel would
     * break the circular dependency (room → reservation → room) at the cost of
     * extra indirection. <em>Stage 3 action:</em> introduce an OccupancyPort in
     * the shared kernel, implemented by the reservation module, and remove this
     * cross-schema subquery.
     */
    @Query(value = "SELECT r.* FROM room r" +
                   " WHERE r.status = 'AVAILABLE'" +
                   " AND r.id NOT IN (" +
                   "   SELECT res.room_id FROM reservation res" +
                   "   WHERE res.status IN ('CONFIRMED','CHECKED_IN')" +
                   "     AND res.check_in_date  < :checkOut" +
                   "     AND res.check_out_date > :checkIn" +
                   " ) ORDER BY r.floor, r.room_number",
           nativeQuery = true)
    List<RoomJpaEntity> findAvailable(
            @Param("checkIn")  LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );

    /**
     * Single-row EXISTS check for one specific room. Used by
     * {@code RoomRepositoryImpl.isAvailable()} to avoid loading the full
     * available-room list just to verify one entry.
     *
     * <p><strong>R3-04 NOTE — Cross-module SQL dependency:</strong>
     * Same cross-schema concern as {@link #findAvailable} above.
     * Stage 3: replace subquery with an OccupancyPort call.
     */
    @Query(value = "SELECT CASE WHEN COUNT(r.id) > 0 THEN TRUE ELSE FALSE END" +
                   " FROM room r" +
                   " WHERE r.id = :roomId" +
                   "   AND r.status = 'AVAILABLE'" +
                   "   AND r.id NOT IN (" +
                   "     SELECT res.room_id FROM reservation res" +
                   "     WHERE res.status IN ('CONFIRMED','CHECKED_IN')" +
                   "       AND res.check_in_date  < :checkOut" +
                   "       AND res.check_out_date > :checkIn" +
                   "   )",
           nativeQuery = true)
    boolean existsAvailableRoom(
            @Param("roomId")   UUID      roomId,
            @Param("checkIn")  LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );
}
