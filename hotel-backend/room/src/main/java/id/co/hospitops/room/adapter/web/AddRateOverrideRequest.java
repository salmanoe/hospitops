package id.co.hospitops.room.adapter.web;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
public record AddRateOverrideRequest(@NotBlank @Size(max=100) String name, @NotNull @Positive BigDecimal priceOverride, @NotNull LocalDate validFrom, @NotNull LocalDate validUntil) {}
