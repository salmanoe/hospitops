package id.co.hospitops.reservation.infrastructure.adapter;

import id.co.hospitops.guest.domain.port.in.ManageGuestUseCase;
import id.co.hospitops.reservation.domain.port.out.DisplayEnrichmentPort;
import id.co.hospitops.room.domain.port.in.ManageRoomUseCase;
import id.co.hospitops.shared.GuestId;
import id.co.hospitops.shared.RoomId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Stage 1+2: direct calls. Stage 3: replace with HTTP clients.
 */
@Component
@RequiredArgsConstructor
public class DisplayEnrichmentAdapter implements DisplayEnrichmentPort {

    private final ManageGuestUseCase guestService;
    private final ManageRoomUseCase roomService;

    @Override
    public GuestDisplay findGuestDisplay(GuestId id) {
        var g = guestService.findById(id);
        return new GuestDisplay(g.fullName(), g.idNumber());
    }

    @Override
    public RoomDisplay findRoomDisplay(RoomId id) {
        var r = roomService.findById(id);
        return new RoomDisplay(r.roomNumber(), r.roomTypeName());
    }
}
