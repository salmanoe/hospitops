package id.co.hospitops.reservation.application.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Hotel revenue analytics for the window {@code [from, to)}.
 *
 * <ul>
 *   <li><b>ADR</b> (Average Daily Rate) = room revenue ÷ room-nights sold.</li>
 *   <li><b>RevPAR</b> (Revenue Per Available Room) = room revenue ÷ available
 *       room-nights = ADR × occupancy.</li>
 *   <li><b>occupancyRate</b> = room-nights sold ÷ available room-nights, as a
 *       percentage (0–100).</li>
 * </ul>
 *
 * Only CONFIRMED, CHECKED_IN and CHECKED_OUT reservations count as "sold"; each
 * contributes only the nights that fall inside the window.
 */
public record RevenueMetricsResponse(
        LocalDate from,
        LocalDate to,
        long days,
        long totalRooms,
        long roomNightsSold,
        long availableRoomNights,
        BigDecimal roomRevenue,
        BigDecimal adr,
        BigDecimal revpar,
        double occupancyRate
) {
}
