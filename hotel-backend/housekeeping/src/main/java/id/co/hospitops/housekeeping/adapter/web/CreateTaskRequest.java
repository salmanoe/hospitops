package id.co.hospitops.housekeeping.adapter.web;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

// R-04 FIX: Added @NotNull to roomId. Without it, a null roomId would pass
// controller validation and blow up in HousekeepingService.createManualTask()
// with an unhelpful NullPointerException rather than a clear 400 response.
public record CreateTaskRequest(
        @NotNull UUID roomId,
        String notes
) {
}
