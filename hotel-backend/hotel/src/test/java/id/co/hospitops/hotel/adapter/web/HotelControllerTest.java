package id.co.hospitops.hotel.adapter.web;

import id.co.hospitops.hotel.application.response.HotelResponse;
import id.co.hospitops.hotel.domain.model.HotelStatus;
import id.co.hospitops.hotel.domain.model.SetupStep;
import id.co.hospitops.hotel.domain.port.in.GroupDashboardUseCase;
import id.co.hospitops.hotel.domain.port.in.ManageHotelPolicyUseCase;
import id.co.hospitops.hotel.domain.port.in.ManageHotelUseCase;
import id.co.hospitops.shared.GroupAdminId;
import id.co.hospitops.shared.GroupAdminPrincipal;
import id.co.hospitops.shared.GroupId;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.exception.BusinessRuleViolationException;
import id.co.hospitops.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HotelController.class)
@DisplayName("HotelController")
class HotelControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ManageHotelUseCase hotelUseCase;

    // Sibling controllers in the same adapter.web scan scope — mock their dependencies.
    @MockitoBean
    GroupDashboardUseCase groupDashboardUseCase;
    @MockitoBean
    ManageHotelPolicyUseCase hotelPolicyUseCase;

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

    private HotelResponse setupResponse() {
        return new HotelResponse(
                HOTEL_UUID, GROUP_UUID,
                "Grand Palace", null, "Asia/Jakarta", "IDR", 4,
                LocalTime.of(14, 0), LocalTime.of(12, 0),
                HotelStatus.SETUP, false,
                List.of(SetupStep.values()),
                LocalDateTime.now(), LocalDateTime.now());
    }

    private HotelResponse activeResponse() {
        return new HotelResponse(
                HOTEL_UUID, GROUP_UUID,
                "Grand Palace", "Jl. Sudirman 1", "Asia/Jakarta", "IDR", 4,
                LocalTime.of(14, 0), LocalTime.of(12, 0),
                HotelStatus.ACTIVE, true,
                List.of(),
                LocalDateTime.now(), LocalDateTime.now());
    }

    private HotelResponse suspendedResponse() {
        return new HotelResponse(
                HOTEL_UUID, GROUP_UUID,
                "Grand Palace", "Jl. Sudirman 1", "Asia/Jakarta", "IDR", 4,
                LocalTime.of(14, 0), LocalTime.of(12, 0),
                HotelStatus.SUSPENDED, true,
                List.of(),
                LocalDateTime.now(), LocalDateTime.now());
    }

    // ── POST /api/v1/group/hotels ─────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/group/hotels")
    class CreateHotel {

        @Test
        @DisplayName("returns 201 with SETUP hotel — groupId is taken from JWT, not body")
        void success() throws Exception {
            given(hotelUseCase.createHotel(argThat(cmd ->
                    "Grand Palace".equals(cmd.name()) && GROUP_UUID.equals(cmd.groupId().value()))))
                    .willReturn(setupResponse());

            mockMvc.perform(post("/api/v1/group/hotels")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Grand Palace\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("SETUP"))
                    .andExpect(jsonPath("$.data.checklistComplete").value(false))
                    .andExpect(jsonPath("$.data.remainingSetupSteps").isArray());
        }

        @Test
        @DisplayName("returns 400 when name is blank")
        void blankName() throws Exception {
            mockMvc.perform(post("/api/v1/group/hotels")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"\"}"))
                    .andExpect(status().isBadRequest());

            then(hotelUseCase).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("returns 400 when name is missing from body")
        void missingName() throws Exception {
            mockMvc.perform(post("/api/v1/group/hotels")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());

            then(hotelUseCase).shouldHaveNoInteractions();
        }
    }

    // ── GET /api/v1/group/hotels/{hotelId} ────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/group/hotels/{hotelId}")
    class GetHotel {

        @Test
        @DisplayName("returns 200 with hotel data")
        void success() throws Exception {
            given(hotelUseCase.findById(HotelId.of(HOTEL_UUID)))
                    .willReturn(setupResponse());

            mockMvc.perform(get("/api/v1/group/hotels/{id}", HOTEL_UUID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(HOTEL_UUID.toString()))
                    .andExpect(jsonPath("$.data.name").value("Grand Palace"));
        }

        @Test
        @DisplayName("returns 404 when hotel does not exist")
        void notFound() throws Exception {
            given(hotelUseCase.findById(any()))
                    .willThrow(new ResourceNotFoundException("Hotel", HOTEL_UUID));

            mockMvc.perform(get("/api/v1/group/hotels/{id}", HOTEL_UUID))
                    .andExpect(status().isNotFound());
        }
    }

    // ── GET /api/v1/group/hotels ──────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/group/hotels")
    class ListHotels {

        @Test
        @DisplayName("returns 200 with hotels for the authenticated GROUP_ADMIN's own group")
        void success() throws Exception {
            given(hotelUseCase.findByGroupId(GroupId.of(GROUP_UUID)))
                    .willReturn(List.of(setupResponse()));

            mockMvc.perform(get("/api/v1/group/hotels")
        )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].id").value(HOTEL_UUID.toString()));
        }
    }

    // ── POST /api/v1/group/hotels/{hotelId}/setup/{step} ─────────────────

    @Nested
    @DisplayName("POST /api/v1/group/hotels/{hotelId}/setup/{step}")
    class CompleteSetupStep {

        @Test
        @DisplayName("returns 200 when step is valid and hotel stays in SETUP")
        void stepCompleted() throws Exception {
            given(hotelUseCase.completeSetupStep(
                    argThat(cmd -> SetupStep.PROFILE.equals(cmd.step()))))
                    .willReturn(setupResponse());

            mockMvc.perform(post("/api/v1/group/hotels/{id}/setup/{step}", HOTEL_UUID, "PROFILE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("SETUP"));
        }

        @Test
        @DisplayName("returns 200 with ACTIVE status after the final step triggers activation")
        void autoActivation() throws Exception {
            given(hotelUseCase.completeSetupStep(
                    argThat(cmd -> SetupStep.STAFF_ACCOUNT.equals(cmd.step()))))
                    .willReturn(activeResponse());

            mockMvc.perform(post("/api/v1/group/hotels/{id}/setup/{step}", HOTEL_UUID, "STAFF_ACCOUNT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.data.checklistComplete").value(true))
                    .andExpect(jsonPath("$.data.remainingSetupSteps").isEmpty());
        }

        @Test
        @DisplayName("returns 404 when hotel does not exist")
        void notFound() throws Exception {
            given(hotelUseCase.completeSetupStep(any()))
                    .willThrow(new ResourceNotFoundException("Hotel", HOTEL_UUID));

            mockMvc.perform(post("/api/v1/group/hotels/{id}/setup/{step}", HOTEL_UUID, "PROFILE"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 422 when hotel is not in SETUP status")
        void wrongStatus() throws Exception {
            given(hotelUseCase.completeSetupStep(any()))
                    .willThrow(new BusinessRuleViolationException(
                            "Setup steps can only be completed while the hotel is in SETUP status"));

            mockMvc.perform(post("/api/v1/group/hotels/{id}/setup/{step}", HOTEL_UUID, "PROFILE"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value(
                            "Setup steps can only be completed while the hotel is in SETUP status"));
        }
    }

    // ── POST /api/v1/group/hotels/{hotelId}/suspend ───────────────────────

    @Nested
    @DisplayName("POST /api/v1/group/hotels/{hotelId}/suspend")
    class Suspend {

        @Test
        @DisplayName("returns 200 with SUSPENDED hotel")
        void success() throws Exception {
            given(hotelUseCase.suspend(HotelId.of(HOTEL_UUID))).willReturn(suspendedResponse());

            mockMvc.perform(post("/api/v1/group/hotels/{id}/suspend", HOTEL_UUID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("SUSPENDED"));
        }

        @Test
        @DisplayName("returns 422 when hotel is not ACTIVE")
        void notActive() throws Exception {
            given(hotelUseCase.suspend(any()))
                    .willThrow(new BusinessRuleViolationException("Only an ACTIVE hotel can be suspended"));

            mockMvc.perform(post("/api/v1/group/hotels/{id}/suspend", HOTEL_UUID))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    // ── POST /api/v1/group/hotels/{hotelId}/reactivate ────────────────────

    @Nested
    @DisplayName("POST /api/v1/group/hotels/{hotelId}/reactivate")
    class Reactivate {

        @Test
        @DisplayName("returns 200 with ACTIVE hotel")
        void success() throws Exception {
            given(hotelUseCase.reactivate(HotelId.of(HOTEL_UUID))).willReturn(activeResponse());

            mockMvc.perform(post("/api/v1/group/hotels/{id}/reactivate", HOTEL_UUID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("returns 422 when hotel is not SUSPENDED")
        void notSuspended() throws Exception {
            given(hotelUseCase.reactivate(any()))
                    .willThrow(new BusinessRuleViolationException("Only a SUSPENDED hotel can be reactivated"));

            mockMvc.perform(post("/api/v1/group/hotels/{id}/reactivate", HOTEL_UUID))
                    .andExpect(status().isUnprocessableEntity());
        }
    }
}
