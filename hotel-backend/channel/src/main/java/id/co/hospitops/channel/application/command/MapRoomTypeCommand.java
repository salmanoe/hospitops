package id.co.hospitops.channel.application.command;

import id.co.hospitops.shared.RoomTypeId;

/** Create or update the provider mapping for one room type. */
public record MapRoomTypeCommand(RoomTypeId roomTypeId,
                                 String externalRoomTypeId,
                                 String externalRatePlanId) {
}
