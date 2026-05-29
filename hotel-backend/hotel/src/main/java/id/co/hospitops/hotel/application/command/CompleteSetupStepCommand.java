package id.co.hospitops.hotel.application.command;

import id.co.hospitops.hotel.domain.model.SetupStep;
import id.co.hospitops.shared.HotelId;

public record CompleteSetupStepCommand(
        HotelId hotelId,
        SetupStep step
) {}
