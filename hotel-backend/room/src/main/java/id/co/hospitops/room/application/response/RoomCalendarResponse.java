package id.co.hospitops.room.application.response;

import id.co.hospitops.shared.RoomTypeId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * One room type's availability + rate across a date range — the row data for
 * the rate/availability calendar (channel-manager style grid).
 */
public record RoomCalendarResponse(
        RoomTypeId roomTypeId,
        String name,
        int capacity,
        List<DayCell> days
) {
    /** Availability (sellable units) and rate for a single night. */
    public record DayCell(LocalDate date, int available, BigDecimal rate) {
    }
}
