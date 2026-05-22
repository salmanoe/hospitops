package id.co.hospitops.room.domain.port.out;

import id.co.hospitops.room.domain.model.RoomRateOverride;
import id.co.hospitops.shared.RoomTypeId;

import java.util.List;

public interface RoomRateOverrideRepository {

    RoomRateOverride save(RoomRateOverride override);

    List<RoomRateOverride> findByRoomTypeId(RoomTypeId roomTypeId);
}
