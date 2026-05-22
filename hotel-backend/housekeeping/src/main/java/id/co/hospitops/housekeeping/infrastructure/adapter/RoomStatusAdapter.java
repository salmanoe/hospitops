package id.co.hospitops.housekeeping.infrastructure.adapter;

import id.co.hospitops.housekeeping.domain.port.out.RoomStatusPort;
import id.co.hospitops.room.domain.model.RoomStatus;
import id.co.hospitops.room.domain.port.in.ManageRoomUseCase;
import id.co.hospitops.shared.RoomId;
import id.co.hospitops.shared.exception.BusinessRuleViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Stage 1+2: direct call to room module. Stage 3: replace with HTTP client.
 */
@Component
@RequiredArgsConstructor
public class RoomStatusAdapter implements RoomStatusPort {
    private final ManageRoomUseCase roomService;

    @Override
    public List<RoomBoardEntry> getAllRoomsGroupedByFloor() {
        return roomService.findAll(null, Pageable.unpaged()).content()
                .stream()
                .map(r -> new RoomBoardEntry(r.id(), r.roomNumber(), r.floor(), r.status().name(), r.roomTypeName()))
                .toList();
    }

    @Override
    public void updateRoomStatus(RoomId roomId, String status, String notes) {
        RoomStatus newStatus;
        try {
            newStatus = RoomStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleViolationException("Unknown room status: " + status);
        }
        roomService.changeRoomStatus(roomId, newStatus, notes);
    }
}
