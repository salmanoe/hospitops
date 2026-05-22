package id.co.hospitops.room.application.command;

import jakarta.validation.constraints.Min;

public record UpdateRoomCommand(
        @Min(1) int    floor,
                String notes
) {}
