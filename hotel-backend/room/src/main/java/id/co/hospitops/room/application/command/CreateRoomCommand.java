package id.co.hospitops.room.application.command;

import id.co.hospitops.shared.RoomTypeId;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateRoomCommand(
        @NotBlank           String     roomNumber,
        @Min(1)             int        floor,
        @NotNull            RoomTypeId roomTypeId,
                            String     notes
) {}
