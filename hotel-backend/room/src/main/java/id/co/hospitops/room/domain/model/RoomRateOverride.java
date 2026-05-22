package id.co.hospitops.room.domain.model;

import id.co.hospitops.shared.Money;
import id.co.hospitops.shared.RoomTypeId;

import java.time.LocalDate;
import java.util.UUID;

public record RoomRateOverride(
        UUID       id,
        RoomTypeId roomTypeId,
        String     name,
        Money      priceOverride,
        LocalDate  validFrom,
        LocalDate  validUntil
) {
    public RoomRateOverride {
        if (validUntil.isBefore(validFrom))
            throw new IllegalArgumentException("validUntil must be >= validFrom");
    }

    public boolean isActiveOn(LocalDate date) {
        return !date.isBefore(validFrom) && !date.isAfter(validUntil);
    }
}
