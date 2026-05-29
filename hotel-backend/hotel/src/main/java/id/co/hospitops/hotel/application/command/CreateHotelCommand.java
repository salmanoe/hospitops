package id.co.hospitops.hotel.application.command;

import id.co.hospitops.shared.GroupId;

public record CreateHotelCommand(
        GroupId groupId,
        String name
) {}
