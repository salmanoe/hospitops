package id.co.hospitops.room.adapter.web;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record CreateRoomTypeRequest(@NotBlank @Size(max=100) String name, @Min(1) int capacity, String description, @NotNull @Positive BigDecimal basePrice) {}
