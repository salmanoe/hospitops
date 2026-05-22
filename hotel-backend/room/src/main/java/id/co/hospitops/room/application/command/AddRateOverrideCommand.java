package id.co.hospitops.room.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AddRateOverrideCommand(
        @NotBlank           String     name,
        @NotNull @Positive  BigDecimal priceOverride,
        @NotNull            LocalDate  validFrom,
        @NotNull            LocalDate  validUntil
) {}
