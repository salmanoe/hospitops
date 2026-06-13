package id.co.hospitops.bootstrap;

import id.co.hospitops.hotel.application.HotelSummaryReconciliationJob;
import id.co.hospitops.hotel.domain.model.HotelSummary;
import id.co.hospitops.hotel.domain.port.out.HotelSummaryRepository;
import id.co.hospitops.shared.HotelId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link HotelSummaryReconciliationJob}.
 *
 * <p>Seeds deterministic data into a real PostgreSQL instance, calls
 * {@code reconcileHotel()} directly (bypassing the scheduler), then asserts
 * that the resulting {@link HotelSummary} matches expected computed values.
 *
 * <p>This protects the raw SQL JOINs in the reconciliation job from silently
 * drifting when the schema changes — they are the highest-risk correctness
 * logic in the nightly job.
 */
@Tag("integration")
@SpringBootTest
class HotelSummaryReconciliationJobIntegrationTest {

    // ── Testcontainers ────────────────────────────────────────────────────

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
                "jdbc:postgresql://localhost:" + postgres.getMappedPort(5432)
                        + "/" + postgres.getDatabaseName());
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    // ── Spring beans ──────────────────────────────────────────────────────

    @Autowired
    HotelSummaryReconciliationJob reconciliationJob;
    @Autowired
    HotelSummaryRepository summaryRepository;
    @Autowired
    JdbcTemplate jdbc;

    // ── Fixed IDs ─────────────────────────────────────────────────────────

    static final UUID GROUP_ID      = UUID.fromString("f0000000-0000-0000-0000-000000000001");
    static final UUID HOTEL_ID      = UUID.fromString("f0000000-0000-0000-0000-000000000002");
    static final UUID ROOM_TYPE_ID  = UUID.fromString("f0000000-0000-0000-0000-000000000003");
    static final UUID ROOM_AVAIL_ID = UUID.fromString("f0000000-0000-0000-0000-000000000004");
    static final UUID ROOM_DIRTY_ID = UUID.fromString("f0000000-0000-0000-0000-000000000005");
    static final UUID ROOM_OCCUP_ID = UUID.fromString("f0000000-0000-0000-0000-000000000006");
    static final UUID GUEST_ID      = UUID.fromString("f0000000-0000-0000-0000-000000000007");
    // Reservations: one CHECKED_IN (→ occupiedRooms), one arriving today, one departing today
    static final UUID RSRV_CHECKIN  = UUID.fromString("f0000000-0000-0000-0000-000000000008");
    static final UUID RSRV_ARRIVE   = UUID.fromString("f0000000-0000-0000-0000-000000000009");
    static final UUID RSRV_DEPART   = UUID.fromString("f0000000-0000-0000-0000-000000000010");
    static final UUID INVOICE_ID    = UUID.fromString("f0000000-0000-0000-0000-000000000011");
    static final UUID PAYMENT_ID    = UUID.fromString("f0000000-0000-0000-0000-000000000012");

    @BeforeEach
    void seedData() {
        // Delete in FK-safe order (child tables first), scoped to our fixed IDs.
        jdbc.update("DELETE FROM payment WHERE id = ?", PAYMENT_ID);
        jdbc.update("DELETE FROM invoice WHERE id = ?", INVOICE_ID);
        jdbc.update("DELETE FROM reservation WHERE id IN (?, ?, ?)", RSRV_CHECKIN, RSRV_ARRIVE, RSRV_DEPART);
        jdbc.update("DELETE FROM room WHERE id IN (?, ?, ?)", ROOM_AVAIL_ID, ROOM_DIRTY_ID, ROOM_OCCUP_ID);
        jdbc.update("DELETE FROM room_type WHERE id = ?", ROOM_TYPE_ID);
        jdbc.update("DELETE FROM guest WHERE id = ?", GUEST_ID);
        jdbc.update("DELETE FROM hotel_summary WHERE hotel_id = ?", HOTEL_ID);
        jdbc.update("DELETE FROM hotel WHERE id = ?", HOTEL_ID);

        // Group (idempotent across test re-runs)
        jdbc.update("""
                INSERT INTO "group" (id, name, owner_email, created_at, updated_at)
                VALUES (?, 'Reconcile Group', 'reconcile@test.com', now(), now())
                ON CONFLICT (id) DO NOTHING
                """, GROUP_ID);

        // Hotel
        jdbc.update("""
                INSERT INTO hotel (id, group_id, name, status, version, created_at, updated_at)
                VALUES (?, ?, 'Reconcile Hotel', 'ACTIVE', 0, now(), now())
                """, HOTEL_ID, GROUP_ID);

        // Room type
        jdbc.update("""
                INSERT INTO room_type (id, name, capacity, base_price, hotel_id, created_at, updated_at)
                VALUES (?, 'Reconcile Standard', 2, 300000.00, ?, now(), now())
                """, ROOM_TYPE_ID, HOTEL_ID);

        // 3 rooms: 1 AVAILABLE, 1 DIRTY, 1 OCCUPIED
        jdbc.update("""
                INSERT INTO room (id, room_number, floor, status, room_type_id, hotel_id, created_at, updated_at)
                VALUES (?, '101R', 1, 'AVAILABLE', ?, ?, now(), now())
                """, ROOM_AVAIL_ID, ROOM_TYPE_ID, HOTEL_ID);

        jdbc.update("""
                INSERT INTO room (id, room_number, floor, status, room_type_id, hotel_id, created_at, updated_at)
                VALUES (?, '102R', 1, 'DIRTY', ?, ?, now(), now())
                """, ROOM_DIRTY_ID, ROOM_TYPE_ID, HOTEL_ID);

        jdbc.update("""
                INSERT INTO room (id, room_number, floor, status, room_type_id, hotel_id, created_at, updated_at)
                VALUES (?, '103R', 1, 'OCCUPIED', ?, ?, now(), now())
                """, ROOM_OCCUP_ID, ROOM_TYPE_ID, HOTEL_ID);

        // Guest
        jdbc.update("""
                INSERT INTO guest (id, full_name, hotel_id, created_at, updated_at)
                VALUES (?, 'Reconcile Guest', ?, now(), now())
                """, GUEST_ID, HOTEL_ID);

        LocalDate today = LocalDate.now();

        // CHECKED_IN reservation (occupiedRooms += 1)
        jdbc.update("""
                INSERT INTO reservation (id, hotel_id, guest_id, room_id, check_in_date, check_out_date,
                    adults, children, status, reservation_number, rate_per_night, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, 2, 0, 'CHECKED_IN', 'REC-R001', 300000.00, now(), now())
                """, RSRV_CHECKIN, HOTEL_ID, GUEST_ID, ROOM_OCCUP_ID,
                today.minusDays(1), today.plusDays(2));

        // CONFIRMED reservation with check_in_date = today (arrivalsToday += 1)
        jdbc.update("""
                INSERT INTO reservation (id, hotel_id, guest_id, room_id, check_in_date, check_out_date,
                    adults, children, status, reservation_number, rate_per_night, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, 2, 0, 'CONFIRMED', 'REC-R002', 300000.00, now(), now())
                """, RSRV_ARRIVE, HOTEL_ID, GUEST_ID, ROOM_AVAIL_ID,
                today, today.plusDays(3));

        // CONFIRMED reservation with check_out_date = today (departuresToday += 1)
        jdbc.update("""
                INSERT INTO reservation (id, hotel_id, guest_id, room_id, check_in_date, check_out_date,
                    adults, children, status, reservation_number, rate_per_night, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, 2, 0, 'CONFIRMED', 'REC-R003', 300000.00, now(), now())
                """, RSRV_DEPART, HOTEL_ID, GUEST_ID, ROOM_DIRTY_ID,
                today.minusDays(2), today);

        // Invoice linked to the CHECKED_IN reservation
        jdbc.update("""
                INSERT INTO invoice (id, hotel_id, reservation_id, reservation_number, guest_name,
                    subtotal, tax_amount, discount_amount, total_amount,
                    invoice_number, issued_at, updated_at)
                VALUES (?, ?, ?, 'REC-R001', 'Reconcile Guest',
                    150000.00, 0.00, 0.00, 150000.00,
                    'INV-R001', now(), now())
                """, INVOICE_ID, HOTEL_ID, RSRV_CHECKIN);

        // Payment of 150_000 paid today
        jdbc.update("""
                INSERT INTO payment (id, invoice_id, hotel_id, amount, method, paid_at)
                VALUES (?, ?, ?, 150000.00, 'CASH', now())
                """, PAYMENT_ID, INVOICE_ID, HOTEL_ID);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Tests
    // ═════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("reconcileHotel computes correct room counts")
    void reconcileHotel_roomCounts() {
        reconciliationJob.reconcileHotel(HotelId.of(HOTEL_ID));

        HotelSummary summary = requireSummary();

        assertThat(summary.getTotalRooms()).as("totalRooms").isEqualTo(3);
        // occupiedRooms = reservations with status CHECKED_IN (not room.status)
        assertThat(summary.getOccupiedRooms()).as("occupiedRooms").isEqualTo(1);
        // dirtyRooms = rooms with status DIRTY
        assertThat(summary.getDirtyRooms()).as("dirtyRooms").isEqualTo(1);
        // hotelName must be populated from the hotel table
        assertThat(summary.getHotelName()).as("hotelName").isEqualTo("Reconcile Hotel");
    }

    @Test
    @DisplayName("reconcileHotel computes correct arrivals and departures for today")
    void reconcileHotel_arrivalsAndDepartures() {
        reconciliationJob.reconcileHotel(HotelId.of(HOTEL_ID));

        HotelSummary summary = requireSummary();

        // REC-R002: check_in_date = today, status = CONFIRMED (non-CANCELLED)
        assertThat(summary.getArrivalsToday()).as("arrivalsToday").isEqualTo(1);
        // REC-R003: check_out_date = today, status = CONFIRMED (non-CANCELLED)
        assertThat(summary.getDeparturesToday()).as("departuresToday").isEqualTo(1);
    }

    @Test
    @DisplayName("reconcileHotel computes correct revenue from payments made today")
    void reconcileHotel_revenue() {
        reconciliationJob.reconcileHotel(HotelId.of(HOTEL_ID));

        HotelSummary summary = requireSummary();

        assertThat(summary.getRevenueToday())
                .as("revenueToday")
                .isEqualByComparingTo(new BigDecimal("150000.00"));
        assertThat(summary.getRevenueMonth())
                .as("revenueMonth")
                .isEqualByComparingTo(new BigDecimal("150000.00"));
    }

    @Test
    @DisplayName("reconcileHotel creates a new HotelSummary row when none exists")
    void reconcileHotel_createsRowWhenMissing() {
        assertThat(summaryRepository.findByHotelId(HotelId.of(HOTEL_ID)))
                .as("no pre-existing summary")
                .isEmpty();

        reconciliationJob.reconcileHotel(HotelId.of(HOTEL_ID));

        assertThat(summaryRepository.findByHotelId(HotelId.of(HOTEL_ID)))
                .as("summary created by reconcile")
                .isPresent();
    }

    @Test
    @DisplayName("reconcileHotel is idempotent — running twice produces the same result")
    void reconcileHotel_idempotent() {
        reconciliationJob.reconcileHotel(HotelId.of(HOTEL_ID));
        reconciliationJob.reconcileHotel(HotelId.of(HOTEL_ID));

        HotelSummary summary = requireSummary();

        assertThat(summary.getTotalRooms()).as("totalRooms after 2nd run").isEqualTo(3);
        assertThat(summary.getOccupiedRooms()).as("occupiedRooms after 2nd run").isEqualTo(1);
        assertThat(summary.getRevenueToday())
                .as("revenueToday after 2nd run")
                .isEqualByComparingTo(new BigDecimal("150000.00"));
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private HotelSummary requireSummary() {
        return summaryRepository.findByHotelId(HotelId.of(HOTEL_ID))
                .orElseThrow(() -> new AssertionError(
                        "HotelSummary not found for hotel " + HOTEL_ID + " after reconcile"));
    }
}
