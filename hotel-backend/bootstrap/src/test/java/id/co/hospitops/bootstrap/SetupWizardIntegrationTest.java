package id.co.hospitops.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.co.hospitops.identity.infrastructure.security.JwtUtil;
import id.co.hospitops.shared.GroupAdminId;
import id.co.hospitops.shared.GroupId;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 8 gate test — setup wizard end-to-end flow.
 *
 * <p>Verifies the complete hotel onboarding journey:
 * <ol>
 *   <li>A new hotel starts in SETUP status.</li>
 *   <li>Staff login is blocked while the hotel is in SETUP.</li>
 *   <li>Completing all 5 setup steps automatically transitions the hotel to ACTIVE.</li>
 *   <li>Staff login succeeds once the hotel is ACTIVE.</li>
 *   <li>Hotel-scoped operations (reservations) work with the staff token.</li>
 *   <li>Suspending the hotel blocks subsequent staff logins.</li>
 *   <li>Reactivating the hotel re-enables staff login.</li>
 * </ol>
 *
 * <p>Tests run in {@link TestMethodOrder.MethodName} order so each step builds
 * on the state left by the previous one. All state is captured in static fields
 * shared across test methods (the Spring context is reused via {@code @SpringBootTest}).
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SetupWizardIntegrationTest {

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

    @Autowired WebApplicationContext context;
    @Autowired JwtUtil jwtUtil;
    @Autowired JdbcTemplate jdbc;

    /** Plain mapper for extracting scalar fields from response bodies.
     *  No JavaTimeModule needed — we only read UUID/string fields, not dates. */
    private static final ObjectMapper JSON = new ObjectMapper();

    // ── Shared test state (mutated across ordered methods) ────────────────

    /** Set up once in step 1; reused by all subsequent steps. */
    static UUID hotelId;
    /** Staff token obtained after activation; reused by resource steps. */
    static String staffToken;

    // Fixed IDs seeded in @BeforeAll — stable across all test methods.
    static final UUID GROUP_UUID   = UUID.fromString("e1000000-0000-0000-0000-000000000001");
    static final UUID ADMIN_UUID   = UUID.fromString("e1000000-0000-0000-0000-000000000002");
    static final UUID STAFF_UUID   = UUID.fromString("e1000000-0000-0000-0000-000000000010");
    static final UUID ROOM_TYPE_UUID = UUID.fromString("e1000000-0000-0000-0000-000000000020");
    static final UUID ROOM_UUID    = UUID.fromString("e1000000-0000-0000-0000-000000000030");
    static final UUID GUEST_UUID   = UUID.fromString("e1000000-0000-0000-0000-000000000040");

    /** BCrypt hash of "password123" — pre-computed to avoid slow hashing in tests. */
    static final String BCRYPT_HASH =
            "$2a$12$te0zOtNd9lbtvEH2sYLWJukOXbYBQ0xSKr4RG9f2cCc.2NCjGoOFW";
    static final String STAFF_USERNAME = "wizard_staff";
    // Password: "admin123" — same known credential used in V2__seed_data.sql and CrossHotelIsolationTest.
    static final String STAFF_PASSWORD = "admin123";
    static final String STAFF_HASH =
            "$2a$12$te0zOtNd9lbtvEH2sYLWJukOXbYBQ0xSKr4RG9f2cCc.2NCjGoOFW";

    MockMvc mockMvc;

    /** GROUP_ADMIN JWT — no hotel scope (group login). */
    String groupAdminToken;

    @BeforeEach
    void setUpMockMvcAndSeedGroup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Seed group + admin with ON CONFLICT DO NOTHING — idempotent across ordered tests.
        jdbc.update("""
                INSERT INTO "group" (id, name, owner_email, created_at, updated_at)
                VALUES (?, 'Wizard Test Group', 'wizard@test.com', now(), now())
                ON CONFLICT (id) DO NOTHING
                """, GROUP_UUID);

        jdbc.update("""
                INSERT INTO group_admin (id, group_id, email, password_hash, created_at, updated_at)
                VALUES (?, ?, 'wizard@test.com', ?, now(), now())
                ON CONFLICT (id) DO NOTHING
                """, ADMIN_UUID, GROUP_UUID, BCRYPT_HASH);

        // Fresh group-scoped GROUP_ADMIN token for each test (stateless JWT, no DB state).
        groupAdminToken = "Bearer " + jwtUtil.generateGroupAdminToken(
                GroupAdminId.of(ADMIN_UUID),
                GroupId.of(GROUP_UUID),
                "wizard@test.com",
                null, null);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Step 1 — Create hotel → starts in SETUP
    // ═════════════════════════════════════════════════════════════════════

    @Test
    @Order(1)
    @DisplayName("Step 1: creating a hotel starts it in SETUP status")
    void step1_createHotelStartsInSetup() throws Exception {
        String body = """
                {"groupId":"%s","name":"Wizard Test Hotel"}
                """.formatted(GROUP_UUID);

        MvcResult result = mockMvc.perform(post("/api/v1/group/hotels")
                        .header(HttpHeaders.AUTHORIZATION, groupAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("SETUP"))
                .andExpect(jsonPath("$.data.checklistComplete").value(false))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String idStr = JSON.readTree(responseBody)
                .path("data").path("id").asText();
        hotelId = UUID.fromString(idStr);

        assertThat(hotelId).isNotNull();
    }

    // ═════════════════════════════════════════════════════════════════════
    // Step 2 — Staff login blocked while hotel is SETUP
    // ═════════════════════════════════════════════════════════════════════

    @Test
    @Order(2)
    @DisplayName("Step 2: staff login is blocked while hotel is in SETUP status")
    void step2_staffLoginBlockedDuringSetup() throws Exception {
        // Seed the staff row early — login must fail regardless of credential validity.
        seedStaff();

        String loginBody = """
                {"username":"%s","password":"%s"}
                """.formatted(STAFF_USERNAME, STAFF_PASSWORD);

        mockMvc.perform(post("/api/v1/hotels/{hotelId}/auth/login", hotelId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Hotel is not currently active"));
    }

    // ═════════════════════════════════════════════════════════════════════
    // Step 3 — Complete all 5 wizard steps → auto-activates on the final one
    // ═════════════════════════════════════════════════════════════════════

    @Test
    @Order(3)
    @DisplayName("Step 3: completing setup steps 1–4 keeps hotel in SETUP")
    void step3_partialStepsKeepHotelInSetup() throws Exception {
        for (String step : new String[]{"PROFILE", "POLICY", "ROOM_TYPE", "ROOM"}) {
            mockMvc.perform(post("/api/v1/group/hotels/{id}/setup/{step}", hotelId, step)
                            .header(HttpHeaders.AUTHORIZATION, groupAdminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("SETUP"))
                    .andExpect(jsonPath("$.data.checklistComplete").value(false));
        }
    }

    @Test
    @Order(4)
    @DisplayName("Step 4: completing the final setup step auto-activates the hotel")
    void step4_finalStepActivatesHotel() throws Exception {
        mockMvc.perform(post("/api/v1/group/hotels/{id}/setup/{step}",
                        hotelId, "STAFF_ACCOUNT")
                        .header(HttpHeaders.AUTHORIZATION, groupAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.checklistComplete").value(true))
                .andExpect(jsonPath("$.data.remainingSetupSteps").isEmpty());
    }

    // ═════════════════════════════════════════════════════════════════════
    // Step 5 — Staff login succeeds after activation
    // ═════════════════════════════════════════════════════════════════════

    @Test
    @Order(5)
    @DisplayName("Step 5: staff login succeeds once hotel is ACTIVE")
    void step5_staffLoginSucceedsAfterActivation() throws Exception {
        String loginBody = """
                {"username":"%s","password":"%s"}
                """.formatted(STAFF_USERNAME, STAFF_PASSWORD);

        MvcResult result = mockMvc.perform(post("/api/v1/hotels/{hotelId}/auth/login", hotelId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();

        String token = JSON.readTree(result.getResponse().getContentAsString())
                .path("data").path("token").asText();
        staffToken = "Bearer " + token;

        assertThat(staffToken).isNotBlank();
    }

    // ═════════════════════════════════════════════════════════════════════
    // Step 6 — Reservations can be created with the staff token
    // ═════════════════════════════════════════════════════════════════════

    @Test
    @Order(6)
    @DisplayName("Step 6: staff can create a reservation in an ACTIVE hotel")
    void step6_reservationCanBeCreatedAfterActivation() throws Exception {
        seedRoomTypeRoomAndGuest();

        String reservationBody = """
                {
                  "guestId": "%s",
                  "roomId":  "%s",
                  "checkIn":  "%s",
                  "checkOut": "%s",
                  "adults": 2,
                  "children": 0
                }
                """.formatted(
                GUEST_UUID,
                ROOM_UUID,
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(3));

        mockMvc.perform(post("/api/v1/reservations")
                        .header(HttpHeaders.AUTHORIZATION, staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationBody))
                .andExpect(status().isCreated());
    }

    // ═════════════════════════════════════════════════════════════════════
    // Step 7 — Suspending the hotel blocks subsequent logins
    // ═════════════════════════════════════════════════════════════════════

    @Test
    @Order(7)
    @DisplayName("Step 7: suspending the hotel blocks staff login")
    void step7_suspendHotelBlocksLogin() throws Exception {
        // Suspend via GROUP_ADMIN endpoint
        mockMvc.perform(post("/api/v1/group/hotels/{id}/suspend", hotelId)
                        .header(HttpHeaders.AUTHORIZATION, groupAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"));

        // Staff login must now be blocked
        String loginBody = """
                {"username":"%s","password":"%s"}
                """.formatted(STAFF_USERNAME, STAFF_PASSWORD);

        mockMvc.perform(post("/api/v1/hotels/{hotelId}/auth/login", hotelId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Hotel is not currently active"));
    }

    // ═════════════════════════════════════════════════════════════════════
    // Step 8 — Reactivating the hotel re-enables login
    // ═════════════════════════════════════════════════════════════════════

    @Test
    @Order(8)
    @DisplayName("Step 8: reactivating the hotel re-enables staff login")
    void step8_reactivateHotelReenablesLogin() throws Exception {
        mockMvc.perform(post("/api/v1/group/hotels/{id}/reactivate", hotelId)
                        .header(HttpHeaders.AUTHORIZATION, groupAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        String loginBody = """
                {"username":"%s","password":"%s"}
                """.formatted(STAFF_USERNAME, STAFF_PASSWORD);

        mockMvc.perform(post("/api/v1/hotels/{hotelId}/auth/login", hotelId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    // ═════════════════════════════════════════════════════════════════════
    // Step 9 — GROUP_ADMIN /enter is blocked for SUSPENDED hotels
    // ═════════════════════════════════════════════════════════════════════

    @Test
    @Order(9)
    @DisplayName("Step 9: GROUP_ADMIN /enter is blocked for a SUSPENDED hotel")
    void step9_groupAdminEnterBlockedForSuspendedHotel() throws Exception {
        // Suspend again to test the /enter restriction independently
        mockMvc.perform(post("/api/v1/group/hotels/{id}/suspend", hotelId)
                        .header(HttpHeaders.AUTHORIZATION, groupAdminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/group/hotels/{id}/enter", hotelId)
                        .header(HttpHeaders.AUTHORIZATION, groupAdminToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Hotel is not currently active"));
    }

    // ═════════════════════════════════════════════════════════════════════
    // Helpers — seed supporting data after hotelId is known
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Seeds a staff account for the newly created hotel.
     * Called in step 2 (before the checklist is complete) to confirm the
     * login block is driven by hotel status, not missing staff data.
     */
    private void seedStaff() {
        jdbc.update("""
                INSERT INTO staff (id, full_name, username, password_hash, role, active, hotel_id, created_at, updated_at)
                VALUES (?, 'Wizard Staff', ?, ?, 'ADMIN', true, ?, now(), now())
                ON CONFLICT (id) DO NOTHING
                """, STAFF_UUID, STAFF_USERNAME, STAFF_HASH, hotelId);
    }

    /**
     * Seeds a room type, room, and guest for the reservation creation test (step 6).
     * Uses fixed UUIDs so the test is idempotent against repeated context loads.
     */
    private void seedRoomTypeRoomAndGuest() {
        jdbc.update("""
                INSERT INTO room_type (id, name, capacity, base_price, hotel_id, created_at, updated_at)
                VALUES (?, 'Wizard Deluxe', 2, 500000.00, ?, now(), now())
                ON CONFLICT (id) DO NOTHING
                """, ROOM_TYPE_UUID, hotelId);

        jdbc.update("""
                INSERT INTO room (id, room_number, floor, status, room_type_id, hotel_id, created_at, updated_at)
                VALUES (?, '101W', 1, 'AVAILABLE', ?, ?, now(), now())
                ON CONFLICT (id) DO NOTHING
                """, ROOM_UUID, ROOM_TYPE_UUID, hotelId);

        jdbc.update("""
                INSERT INTO guest (id, full_name, hotel_id, created_at, updated_at)
                VALUES (?, 'Wizard Guest', ?, now(), now())
                ON CONFLICT (id) DO NOTHING
                """, GUEST_UUID, hotelId);
    }
}
