package id.co.hospitops.room.application.command;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record UpdateRoomTypeCommand(
        @NotBlank                   String     name,
        @Min(1)                     int        capacity,
                                    String     description,
        @NotNull @PositiveOrZero    BigDecimal basePrice
) {}
