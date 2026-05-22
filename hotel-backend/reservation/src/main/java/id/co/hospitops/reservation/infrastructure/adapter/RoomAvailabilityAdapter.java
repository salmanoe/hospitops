package id.co.hospitops.reservation.infrastructure.adapter;

import id.co.hospitops.reservation.domain.port.out.RoomAvailabilityPort;
import id.co.hospitops.room.domain.port.in.ManageRoomUseCase;
import id.co.hospitops.shared.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * W-2 FIX: Injects {@link ManageRoomUseCase} (the port interface) rather than
 * the concrete {@code RoomService} class, preserving the dependency-inversion rule.
 */
@Component
@RequiredArgsConstructor
public class RoomAvailabilityAdapter implements RoomAvailabilityPort {

    private final ManageRoomUseCase roomUseCase;

    @Override
    public boolean isAvailable(RoomId id, LocalDate ci, LocalDate co) {
        return roomUseCase.isAvailable(id, ci, co);
    }

    @Override
    public Money resolveRate(RoomId id, LocalDate checkIn) {
        return roomUseCase.resolveRate(id, checkIn);
    }
}
