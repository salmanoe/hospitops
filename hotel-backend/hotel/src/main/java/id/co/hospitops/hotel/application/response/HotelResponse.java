package id.co.hospitops.hotel.application.response;

import id.co.hospitops.hotel.domain.model.Hotel;
import id.co.hospitops.hotel.domain.model.HotelStatus;
import id.co.hospitops.hotel.domain.model.SetupStep;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record HotelResponse(
        UUID id,
        UUID groupId,
        String name,
        String address,
        String timezone,
        String currency,
        int starRating,
        LocalTime defaultCheckInTime,
        LocalTime defaultCheckOutTime,
        HotelStatus status,
        boolean checklistComplete,
        List<SetupStep> remainingSetupSteps,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static HotelResponse from(Hotel h) {
        return new HotelResponse(
                h.getId().value(),
                h.getGroupId().value(),
                h.getName(),
                h.getAddress(),
                h.getTimezone(),
                h.getCurrency(),
                h.getStarRating(),
                h.getDefaultCheckInTime(),
                h.getDefaultCheckOutTime(),
                h.getStatus(),
                h.getChecklist().isComplete(),
                h.getChecklist().remainingSteps(),
                h.getCreatedAt(),
                h.getUpdatedAt());
    }
}
