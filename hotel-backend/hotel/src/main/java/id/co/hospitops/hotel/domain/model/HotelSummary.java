package id.co.hospitops.hotel.domain.model;

import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.Money;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Read-model for a single hotel's pre-computed KPIs shown on the group dashboard.
 *
 * <p>Values are maintained incrementally by {@code HotelSummaryEventHandler} and
 * recomputed nightly by {@code HotelSummaryReconciliationJob}. All counts are
 * non-negative; negative increments are ignored with a warning.
 */
@Getter
public class HotelSummary {

    private final HotelId hotelId;
    private String hotelName;
    private int occupiedRooms;
    private int totalRooms;
    private int arrivalsToday;
    private int departuresToday;
    private BigDecimal revenueToday;
    private BigDecimal revenueMonth;
    private int dirtyRooms;
    private LocalDateTime updatedAt;

    public static HotelSummary empty(HotelId hotelId, String hotelName) {
        return new HotelSummary(hotelId, hotelName, 0, 0, 0, 0,
                BigDecimal.ZERO, BigDecimal.ZERO, 0, LocalDateTime.now());
    }

    public static HotelSummary reconstitute(HotelId hotelId,
                                            String hotelName,
                                            int occupiedRooms, int totalRooms,
                                            int arrivalsToday, int departuresToday,
                                            BigDecimal revenueToday, BigDecimal revenueMonth,
                                            int dirtyRooms, LocalDateTime updatedAt) {
        return new HotelSummary(hotelId, hotelName, occupiedRooms, totalRooms,
                arrivalsToday, departuresToday, revenueToday, revenueMonth,
                dirtyRooms, updatedAt);
    }

    private HotelSummary(HotelId hotelId, String hotelName,
                         int occupiedRooms, int totalRooms,
                         int arrivalsToday, int departuresToday,
                         BigDecimal revenueToday, BigDecimal revenueMonth,
                         int dirtyRooms, LocalDateTime updatedAt) {
        this.hotelId = hotelId;
        this.hotelName = hotelName;
        this.occupiedRooms = occupiedRooms;
        this.totalRooms = totalRooms;
        this.arrivalsToday = arrivalsToday;
        this.departuresToday = departuresToday;
        this.revenueToday = revenueToday;
        this.revenueMonth = revenueMonth;
        this.dirtyRooms = dirtyRooms;
        this.updatedAt = updatedAt;
    }

    // ── Incremental mutators (used by the event handler) ─────────────────────

    public void incrementOccupied() {
        occupiedRooms = Math.max(0, occupiedRooms + 1);
        touch();
    }

    public void decrementOccupied() {
        occupiedRooms = Math.max(0, occupiedRooms - 1);
        touch();
    }

    public void incrementTotalRooms() {
        totalRooms = Math.max(0, totalRooms + 1);
        touch();
    }

    public void incrementArrivals() {
        arrivalsToday = Math.max(0, arrivalsToday + 1);
        touch();
    }

    public void incrementDepartures() {
        departuresToday = Math.max(0, departuresToday + 1);
        touch();
    }

    public void incrementDirtyRooms() {
        dirtyRooms = Math.max(0, dirtyRooms + 1);
        touch();
    }

    public void decrementDirtyRooms() {
        dirtyRooms = Math.max(0, dirtyRooms - 1);
        touch();
    }

    public void addRevenue(Money amount) {
        revenueToday = revenueToday.add(amount.amount());
        revenueMonth = revenueMonth.add(amount.amount());
        touch();
    }

    /**
     * Full recompute — replaces all values (used by the nightly reconciliation job).
     * Also refreshes {@code hotelName} so it stays in sync if the name ever changes.
     */
    public void recompute(String hotelName,
                          int occupiedRooms, int totalRooms,
                          int arrivalsToday, int departuresToday,
                          BigDecimal revenueToday, BigDecimal revenueMonth,
                          int dirtyRooms) {
        this.hotelName = hotelName;
        this.occupiedRooms = occupiedRooms;
        this.totalRooms = totalRooms;
        this.arrivalsToday = arrivalsToday;
        this.departuresToday = departuresToday;
        this.revenueToday = revenueToday;
        this.revenueMonth = revenueMonth;
        this.dirtyRooms = dirtyRooms;
        touch();
    }

    private void touch() {
        updatedAt = LocalDateTime.now();
    }
}
