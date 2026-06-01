package id.co.hospitops.hotel.application.response;

import id.co.hospitops.hotel.domain.model.HotelSummary;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record HotelSummaryResponse(
        UUID hotelId,
        String hotelName,
        String hotelStatus,
        int occupiedRooms,
        int totalRooms,
        int arrivalsToday,
        int departuresToday,
        BigDecimal revenueToday,
        BigDecimal revenueMonth,
        int dirtyRooms,
        LocalDateTime updatedAt
) {
    public static HotelSummaryResponse from(HotelSummary s) {
        return new HotelSummaryResponse(
                s.getHotelId().value(),
                s.getHotelName(),
                s.getHotelStatus() != null ? s.getHotelStatus().name() : "SETUP",
                s.getOccupiedRooms(),
                s.getTotalRooms(),
                s.getArrivalsToday(),
                s.getDeparturesToday(),
                s.getRevenueToday(),
                s.getRevenueMonth(),
                s.getDirtyRooms(),
                s.getUpdatedAt());
    }
}
