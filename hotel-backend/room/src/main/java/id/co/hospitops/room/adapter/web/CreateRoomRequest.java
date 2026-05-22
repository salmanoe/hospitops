package id.co.hospitops.room.adapter.web;
import jakarta.validation.constraints.*;
import java.util.UUID;
public record CreateRoomRequest(@NotBlank @Size(max=10) String roomNumber, @Min(1) int floor, @NotNull UUID roomTypeId, String notes) {}
