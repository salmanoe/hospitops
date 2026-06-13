package id.co.hospitops.channel.adapter.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Manual ARI push for a room type: availability + rate per night. */
public record PushAriRequest(@NotEmpty @Valid List<Night> nights) {

    public record Night(
            @NotNull LocalDate date,
            @Min(0) int availability,
            @NotNull @PositiveOrZero BigDecimal rate) {
    }
}
