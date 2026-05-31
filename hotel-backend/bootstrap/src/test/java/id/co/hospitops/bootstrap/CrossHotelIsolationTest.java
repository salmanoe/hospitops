package id.co.hospitops.bootstrap;

import id.co.hospitops.identity.domain.model.Staff;
import id.co.hospitops.identity.domain.model.StaffRole;
import id.co.hospitops.identity.infrastructure.security.JwtUtil;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.StaffId;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 5 gate tests — cross-hotel data isolation.
 *
 * <p>Flat structure (no {@code @Nested}) so all tests share a single
 * Spring {@code ApplicationContext} and one Testcontainers PostgreSQL instance.
 *
 * <p>Verifies two Phase 5 invariants:
 * <ol>
 *   <li>Hotel A's JWT cannot retrieve Hotel B's resources and vice versa.</li>
 *   <li>An authenticated caller with no hotel-scoped JWT gets HTTP 403 on every
 *       endpoint annotated with {@link id.co.hospitops.shared.web.RequiresHotelContext}.</li>
 * </ol>
 */
@SpringBootTest
class CrossHotelIsolationTest {

    // ── Container ─────────────────────────────────────────────────────────
    // Started eagerly in a static initializer so it is guaranteed running
    // before Spring evaluates any @DynamicPropertySource supplier. The Ryuk
    // reaper container handles cleanup automatically after the JVM exits.

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        // On Windows with Docker Desktop, postgres.getJdbcUrl() resolves the host
        // to the Docker named-pipe path (npipe:////./pipe/...) which the PostgreSQL
        // JDBC driver rejects.  Building the URL from the mapped port forces localhost.
        registry.add("spring.datasource.url", () ->
                "jdbc:postgresql://localhost:" + postgres.getMappedPort(5432)
                        + "/" + postgres.getDatabaseName());
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    // ── Spring beans ──────────────────────────────────────────────────────

    @Autowired
    WebApplicationContext context;
    @Autowired
    JwtUtil jwtUtil;
    @Autowired
    JdbcTemplate jdbc;

    // ── Per-test state ────────────────────────────────────────────────────

    MockMvc mockMvc;
    String tokenA;
    String tokenB;

    // ── Fixed IDs — stable across @BeforeEach runs (ON CONFLICT DO NOTHING) ─

    static final UUID HOTEL_A_ID = UUID.fromString("b0000000-0000-0000-0000-000000000002");
    static final UUID HOTEL_B_ID = UUID.fromString("c0000000-0000-0000-0000-000000000001");
    static final UUID ROOM_TYPE_A_ID = UUID.fromString("c0000000-0000-0000-0000-000000000002");
    static final UUID ROOM_TYPE_B_ID = UUID.fromString("c0000000-0000-0000-0000-000000000003");
    static final UUID ROOM_A_ID = UUID.fromString("c0000000-0000-0000-0000-000000000004");
    static final UUID ROOM_B_ID = UUID.fromString("c0000000-0000-0000-0000-000000000005");
    static final UUID GUEST_A_ID = UUID.fromString("c0000000-0000-0000-0000-000000000006");
    static final UUID GUEST_B_ID = UUID.fromString("c0000000-0000-0000-0000-000000000007");
    static final UUID STAFF_A_ID = UUID.fromString("c0000000-0000-0000-0000-000000000008");
    static final UUID STAFF_B_ID = UUID.fromString("c0000000-0000-0000-0000-000000000009");
    static final String BCRYPT_HASH =
            "$2a$12$te0zOtNd9lbtvEH2sYLWJukOXbYBQ0xSKr4RG9f2cCc.2NCjGoOFW";

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        UUID groupId = UUID.fromString("b0000000-0000-0000-0000-000000000001");

        // Explicitly seed both hotels so this test is self-contained and does not
        // rely on V14 migration seed data for Hotel A. ON CONFLICT DO NOTHING is safe
        // across @BeforeEach runs since the IDs are stable constants.
        jdbc.update("INSERT INTO hotel (id, group_id, name, status, version, created_at, updated_at) " +
                        "VALUES (?, ?, 'Hotel A', 'ACTIVE', 0, now(), now()) ON CONFLICT (id) DO NOTHING",
                HOTEL_A_ID, groupId);

        jdbc.update("INSERT INTO hotel (id, group_id, name, status, version, created_at, updated_at) " +
                        "VALUES (?, ?, 'Hotel B', 'ACTIVE', 0, now(), now()) ON CONFLICT (id) DO NOTHING",
                HOTEL_B_ID, groupId);

        jdbc.update("INSERT INTO room_type (id, name, capacity, base_price, hotel_id, created_at, updated_at) " +
                        "VALUES (?, 'IsoStdA', 2, 400000.00, ?, now(), now()) ON CONFLICT (id) DO NOTHING",
                ROOM_TYPE_A_ID, HOTEL_A_ID);

        jdbc.update("INSERT INTO room_type (id, name, capacity, base_price, hotel_id, created_at, updated_at) " +
                        "VALUES (?, 'IsoStdB', 2, 400000.00, ?, now(), now()) ON CONFLICT (id) DO NOTHING",
                ROOM_TYPE_B_ID, HOTEL_B_ID);

        jdbc.update("INSERT INTO room (id, room_number, floor, status, room_type_id, hotel_id, created_at, updated_at) " +
                        "VALUES (?, '901A', 9, 'AVAILABLE', ?, ?, now(), now()) ON CONFLICT (id) DO NOTHING",
                ROOM_A_ID, ROOM_TYPE_A_ID, HOTEL_A_ID);

        jdbc.update("INSERT INTO room (id, room_number, floor, status, room_type_id, hotel_id, created_at, updated_at) " +
                        "VALUES (?, '901B', 9, 'AVAILABLE', ?, ?, now(), now()) ON CONFLICT (id) DO NOTHING",
                ROOM_B_ID, ROOM_TYPE_B_ID, HOTEL_B_ID);

        jdbc.update("INSERT INTO guest (id, full_name, hotel_id, created_at, updated_at) " +
                        "VALUES (?, 'Guest Alpha', ?, now(), now()) ON CONFLICT (id) DO NOTHING",
                GUEST_A_ID, HOTEL_A_ID);

        jdbc.update("INSERT INTO guest (id, full_name, hotel_id, created_at, updated_at) " +
                        "VALUES (?, 'Guest Beta', ?, now(), now()) ON CONFLICT (id) DO NOTHING",
                GUEST_B_ID, HOTEL_B_ID);

        jdbc.update("INSERT INTO staff (id, full_name, username, password_hash, role, active, hotel_id, created_at, updated_at) " +
                        "VALUES (?, 'Staff A', 'staff_iso_a', ?, 'ADMIN', true, ?, now(), now()) ON CONFLICT (id) DO NOTHING",
                STAFF_A_ID, BCRYPT_HASH, HOTEL_A_ID);

        jdbc.update("INSERT INTO staff (id, full_name, username, password_hash, role, active, hotel_id, created_at, updated_at) " +
                        "VALUES (?, 'Staff B', 'staff_iso_b', ?, 'ADMIN', true, ?, now(), now()) ON CONFLICT (id) DO NOTHING",
                STAFF_B_ID, BCRYPT_HASH, HOTEL_B_ID);

        Staff staffA = Staff.reconstitute(StaffId.of(STAFF_A_ID), "Staff A", "staff_iso_a",
                BCRYPT_HASH, StaffRole.ADMIN, true, HotelId.of(HOTEL_A_ID),
                LocalDateTime.now(), LocalDateTime.now());
        Staff staffB = Staff.reconstitute(StaffId.of(STAFF_B_ID), "Staff B", "staff_iso_b",
                BCRYPT_HASH, StaffRole.ADMIN, true, HotelId.of(HOTEL_B_ID),
                LocalDateTime.now(), LocalDateTime.now());

        tokenA = "Bearer " + jwtUtil.generate(staffA);
        tokenB = "Bearer " + jwtUtil.generate(staffB);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Room isolation
    // ═════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("[Room] Hotel A token reads own room — 200")
    void hotelAReadsOwnRoom() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/{id}", ROOM_A_ID)
                        .header(HttpHeaders.AUTHORIZATION, tokenA))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("[Room] Hotel A token cannot read Hotel B room — 404")
    void hotelACannotReadHotelBRoom() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/{id}", ROOM_B_ID)
                        .header(HttpHeaders.AUTHORIZATION, tokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("[Room] Hotel B token cannot read Hotel A room — 404")
    void hotelBCannotReadHotelARoom() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/{id}", ROOM_A_ID)
                        .header(HttpHeaders.AUTHORIZATION, tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("[Room] Hotel B token reads own room — 200")
    void hotelBReadsOwnRoom() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/{id}", ROOM_B_ID)
                        .header(HttpHeaders.AUTHORIZATION, tokenB))
                .andExpect(status().isOk());
    }

    // ═════════════════════════════════════════════════════════════════════
    // Guest isolation
    // ═════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("[Guest] Hotel A token reads own guest — 200")
    void hotelAReadsOwnGuest() throws Exception {
        mockMvc.perform(get("/api/v1/guests/{id}", GUEST_A_ID)
                        .header(HttpHeaders.AUTHORIZATION, tokenA))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("[Guest] Hotel A token cannot read Hotel B guest — 404")
    void hotelACannotReadHotelBGuest() throws Exception {
        mockMvc.perform(get("/api/v1/guests/{id}", GUEST_B_ID)
                        .header(HttpHeaders.AUTHORIZATION, tokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("[Guest] Hotel B token cannot read Hotel A guest — 404")
    void hotelBCannotReadHotelAGuest() throws Exception {
        mockMvc.perform(get("/api/v1/guests/{id}", GUEST_A_ID)
                        .header(HttpHeaders.AUTHORIZATION, tokenB))
                .andExpect(status().isNotFound());
    }

    // ═════════════════════════════════════════════════════════════════════
    // @RequiresHotelContext — authenticated, no hotel scope → 403
    // ═════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("[403] Room list blocked without hotel scope")
    void roomListBlockedWithoutHotelScope() throws Exception {
        mockMvc.perform(get("/api/v1/rooms").with(user("ga").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("[403] Room by ID blocked without hotel scope")
    void roomByIdBlockedWithoutHotelScope() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/{id}", UUID.randomUUID()).with(user("ga").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("[403] Guest list blocked without hotel scope")
    void guestListBlockedWithoutHotelScope() throws Exception {
        mockMvc.perform(get("/api/v1/guests").with(user("ga").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("[403] Reservation list blocked without hotel scope")
    void reservationListBlockedWithoutHotelScope() throws Exception {
        mockMvc.perform(get("/api/v1/reservations").with(user("ga").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("[403] Invoice list blocked without hotel scope")
    void invoiceListBlockedWithoutHotelScope() throws Exception {
        mockMvc.perform(get("/api/v1/invoices").with(user("ga").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("[403] Housekeeping board blocked without hotel scope")
    void housekeepingBoardBlockedWithoutHotelScope() throws Exception {
        mockMvc.perform(get("/api/v1/housekeeping/board").with(user("ga").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("[403] Staff list blocked without hotel scope")
    void staffListBlockedWithoutHotelScope() throws Exception {
        mockMvc.perform(get("/api/v1/staff").with(user("ga").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }
}
