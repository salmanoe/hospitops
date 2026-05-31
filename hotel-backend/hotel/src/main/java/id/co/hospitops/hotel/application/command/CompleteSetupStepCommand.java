package id.co.hospitops.hotel.application.command;

import id.co.hospitops.hotel.domain.model.SetupStep;
import id.co.hospitops.shared.GroupId;
import id.co.hospitops.shared.HotelId;

public record CompleteSetupStepCommand(
        GroupId callerGroupId,
        HotelId hotelId,
        SetupStep step
) {}
