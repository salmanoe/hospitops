package id.co.hospitops.room.domain.port.in;

import id.co.hospitops.room.application.command.AddRateOverrideCommand;
import id.co.hospitops.room.application.command.CreateRoomTypeCommand;
import id.co.hospitops.room.application.command.UpdateRoomTypeCommand;
import id.co.hospitops.room.application.response.RoomTypeResponse;
import id.co.hospitops.shared.RoomTypeId;
import id.co.hospitops.shared.web.PageResult;
import org.springframework.data.domain.Pageable;

public interface ManageRoomTypeUseCase {

    RoomTypeResponse createRoomType(CreateRoomTypeCommand cmd);

    RoomTypeResponse updateRoomType(RoomTypeId id, UpdateRoomTypeCommand cmd);

    RoomTypeResponse findRoomTypeById(RoomTypeId id);

    PageResult<RoomTypeResponse> findAllRoomTypes(Pageable pageable);

    /** Attach a time-bounded price override to a room type. */
    RoomTypeResponse addRateOverride(RoomTypeId id, AddRateOverrideCommand cmd);
}
