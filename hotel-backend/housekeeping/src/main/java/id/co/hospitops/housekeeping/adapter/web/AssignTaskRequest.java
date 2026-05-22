package id.co.hospitops.housekeeping.adapter.web;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

// R-04 FIX: Added @NotNull to staffId. Without it, a null staffId would pass
// controller validation and reach HousekeepingTask.assign(), which would record
// a null assignee silently instead of returning a descriptive 400 error.
public record AssignTaskRequest(
        @NotNull UUID staffId
) {
}
