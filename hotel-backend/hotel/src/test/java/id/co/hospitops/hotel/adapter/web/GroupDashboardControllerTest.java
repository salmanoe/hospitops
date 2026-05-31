package id.co.hospitops.hotel.adapter.web;

import id.co.hospitops.hotel.application.response.HotelSummaryResponse;
import id.co.hospitops.hotel.domain.port.in.GroupDashboardUseCase;
import id.co.hospitops.hotel.domain.port.in.ManageHotelUseCase;
import id.co.hospitops.shared.GroupAdminId;
import id.co.hospitops.shared.GroupAdminPrincipal;
import id.co.hospitops.shared.GroupId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GroupDashboardController.class)
@DisplayName("GroupDashboardController")
class GroupDashboardControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    GroupDashboardUseCase dashboardUseCase;

    // Sibling controller in the same scan scope — provide its dependency.
    @MockitoBean
    ManageHotelUseCase manageHotelUseCase;

    private static final UUID GROUP_UUID = UUID.randomUUID();
    private static final UUID HOTEL_UUID = UUID.randomUUID();
    private static final UUID ADMIN_UUID = UUID.randomUUID();

    @BeforeEach
    void setUpSecurityContext() {
        var principal = new GroupAdminPrincipal(
                GroupAdminId.of(ADMIN_UUID), GroupId.of(GROUP_UUID),
                "admin@hospitops.local", null);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private HotelSummaryResponse summaryResponse() {
        return new HotelSummaryResponse(
                HOTEL_UUID,
                5, 20, 3, 2,
                new BigDecimal("1500000.00"), new BigDecimal("45000000.00"),
                1, LocalDateTime.now()
        );
    }

    // ── GET /api/v1/group/dashboard ───────────────────────────────────────

    @Test
    @DisplayName("returns 200 with summaries using groupId from the authenticated principal")
    void returnsDashboardForAuthenticatedGroup() throws Exception {
        given(dashboardUseCase.getDashboard(GroupId.of(GROUP_UUID)))
                .willReturn(List.of(summaryResponse()));

        mockMvc.perform(get("/api/v1/group/dashboard")
)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].hotelId").value(HOTEL_UUID.toString()))
                .andExpect(jsonPath("$.data[0].occupiedRooms").value(5));

        // Verify the use case was called with the principal's groupId, not a param
        then(dashboardUseCase).should().getDashboard(GroupId.of(GROUP_UUID));
    }

    @Test
    @DisplayName("cannot query a different group's dashboard — groupId is not a parameter")
    void groupIdIsNotAcceptedAsQueryParam() throws Exception {
        UUID foreignGroupId = UUID.randomUUID();

        // Even if a caller passes a foreign groupId as a query param, it is ignored —
        // the endpoint no longer accepts @RequestParam groupId.
        // The use case will be called with the principal's groupId, not the param.
        given(dashboardUseCase.getDashboard(GroupId.of(GROUP_UUID)))
                .willReturn(List.of(summaryResponse()));

        mockMvc.perform(get("/api/v1/group/dashboard")
                        .param("groupId", foreignGroupId.toString())  // should be ignored
)
                .andExpect(status().isOk());

        // The use case must have been called with the principal's own groupId,
        // never with the foreign UUID passed in the query param.
        then(dashboardUseCase).should().getDashboard(GroupId.of(GROUP_UUID));
        then(dashboardUseCase).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("returns empty list when the group has no hotels")
    void emptyDashboard() throws Exception {
        given(dashboardUseCase.getDashboard(GroupId.of(GROUP_UUID)))
                .willReturn(List.of());

        mockMvc.perform(get("/api/v1/group/dashboard")
)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
