package id.co.hospitops.hotel.application;

import id.co.hospitops.hotel.domain.model.Hotel;
import id.co.hospitops.hotel.domain.model.HotelStatus;
import id.co.hospitops.hotel.domain.model.HotelSummary;
import id.co.hospitops.hotel.domain.port.out.HotelRepository;
import id.co.hospitops.hotel.domain.port.out.HotelSummaryRepository;
import id.co.hospitops.shared.HotelId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Nightly reconciliation job that recomputes {@code hotel_summary} values from
 * authoritative source tables.
 *
 * <p>This is the correctness safety net for the incremental event-driven updates
 * in {@link HotelSummaryEventHandler}. If an event is missed, replayed, or a
 * handler fails silently, this job brings the summary back into sync within 24 h.
 *
 * <p>Runs at 02:00 local server time daily. The job is idempotent — running it
 * multiple times produces the same result.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HotelSummaryReconciliationJob {

    private final HotelRepository hotelRepo;
    private final HotelSummaryRepository summaryRepo;
    private final JdbcTemplate jdbc;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void reconcileAll() {
        log.info("hotel_summary reconciliation: starting nightly full recompute");
        List<Hotel> hotels = hotelRepo.findAll();
        for (Hotel hotel : hotels) {
            try {
                reconcileHotel(hotel.getId());
            } catch (Exception ex) {
                log.error("hotel_summary reconciliation: failed for hotel {} — skipping",
                        hotel.getId(), ex);
            }
        }
        log.info("hotel_summary reconciliation: completed for {} hotels", hotels.size());
    }

    /**
     * Recomputes and persists the summary for a single hotel.
     *
     * <p>Public to allow direct invocation from integration tests in {@code bootstrap/},
     * which cannot share the package {@code id.co.hospitops.hotel.application}.
     */
    @Transactional
    public void reconcileHotel(HotelId hotelId) {
        String id = hotelId.value().toString();
        LocalDate today = LocalDate.now();

        int occupiedRooms = queryInt(
                "SELECT COUNT(*) FROM reservation WHERE hotel_id = ?::uuid AND status = 'CHECKED_IN'", id);

        int totalRooms = queryInt(
                "SELECT COUNT(*) FROM room WHERE hotel_id = ?::uuid", id);

        int arrivalsToday = queryInt(
                "SELECT COUNT(*) FROM reservation WHERE hotel_id = ?::uuid AND check_in_date = ? AND status != 'CANCELLED'",
                id, today);

        int departuresToday = queryInt(
                "SELECT COUNT(*) FROM reservation WHERE hotel_id = ?::uuid AND check_out_date = ? AND status != 'CANCELLED'",
                id, today);

        BigDecimal revenueToday = queryDecimal(
                "SELECT COALESCE(SUM(amount), 0) FROM payment p " +
                        "JOIN invoice i ON i.id = p.invoice_id " +
                        "WHERE i.hotel_id = ?::uuid AND DATE(p.paid_at) = ?",
                id, today);

        BigDecimal revenueMonth = queryDecimal(
                "SELECT COALESCE(SUM(amount), 0) FROM payment p " +
                        "JOIN invoice i ON i.id = p.invoice_id " +
                        "WHERE i.hotel_id = ?::uuid AND DATE_TRUNC('month', p.paid_at) = DATE_TRUNC('month', CURRENT_DATE)",
                id);

        int dirtyRooms = queryInt(
                "SELECT COUNT(*) FROM room WHERE hotel_id = ?::uuid AND status = 'DIRTY'", id);

        String rawHotelName = jdbc.queryForObject(
                "SELECT name FROM hotel WHERE id = ?::uuid", String.class, id);
        final String hotelName = rawHotelName != null ? rawHotelName : "";

        String rawStatus = jdbc.queryForObject(
                "SELECT status FROM hotel WHERE id = ?::uuid", String.class, id);
        final HotelStatus hotelStatus = rawStatus != null
                ? HotelStatus.valueOf(rawStatus)
                : HotelStatus.SETUP;

        HotelSummary summary = summaryRepo.findByHotelId(hotelId)
                .orElseGet(() -> HotelSummary.empty(hotelId, hotelName));

        summary.recompute(hotelName, hotelStatus, occupiedRooms, totalRooms, arrivalsToday, departuresToday,
                revenueToday, revenueMonth, dirtyRooms);

        summaryRepo.save(summary);
        log.debug("hotel_summary reconciliation: recomputed for hotel {}", hotelId);
    }

    private int queryInt(String sql, Object... args) {
        Integer result = jdbc.queryForObject(sql, Integer.class, args);
        return result != null ? result : 0;
    }

    private BigDecimal queryDecimal(String sql, Object... args) {
        BigDecimal result = jdbc.queryForObject(sql, BigDecimal.class, args);
        return result != null ? result : BigDecimal.ZERO;
    }
}
