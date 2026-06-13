package id.co.hospitops.identity.adapter.web;

import id.co.hospitops.identity.application.response.LoginResponse;
import id.co.hospitops.identity.domain.model.StaffRole;
import id.co.hospitops.identity.domain.port.in.AuthUseCase;
import id.co.hospitops.identity.domain.port.in.HotelAuthUseCase;
import id.co.hospitops.identity.domain.port.in.ManageStaffUseCase;
import id.co.hospitops.identity.domain.port.out.StaffRepository;
import id.co.hospitops.identity.domain.port.out.TokenBlacklist;
import id.co.hospitops.identity.infrastructure.security.JwtUtil;
import id.co.hospitops.shared.StaffId;
import id.co.hospitops.shared.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HotelAuthController.class)
@DisplayName("HotelAuthController — /api/v1/hotels/{hotelId}/auth/login")
class HotelAuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    HotelAuthUseCase hotelAuthUseCase;

    // IdentityController is in the same adapter.web scan scope — mock its dependencies.
    @MockitoBean
    AuthUseCase authUseCase;
    @MockitoBean
    ManageStaffUseCase manageStaffUseCase;

    // JwtAuthFilter dependencies — present so the context loads if JwtAuthFilter is scanned
    @MockitoBean
    JwtUtil jwtUtil;
    @MockitoBean
    TokenBlacklist tokenBlacklist;
    @MockitoBean
    StaffRepository staffRepository;

    private static final UUID HOTEL_ID = UUID.randomUUID();

    private static LoginResponse mockLogin() {
        return new LoginResponse(
                "hotel-jwt", "Bearer", 28_800L,
                "refresh-uuid", 604_800L,
                StaffId.generate(), "Alice Smith", "alice", StaffRole.FRONT_DESK);
    }

    @Nested
    @DisplayName("POST /api/v1/hotels/{hotelId}/auth/login")
    class Login {

        @Test
        @DisplayName("returns 200 with hotel-scoped token on valid credentials")
        void success() throws Exception {
            given(hotelAuthUseCase.login(argThat(cmd ->
                    cmd.hotelId().equals(HOTEL_ID) && "alice".equals(cmd.username()))))
                    .willReturn(mockLogin());

            mockMvc.perform(post("/api/v1/hotels/{hotelId}/auth/login", HOTEL_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"alice\",\"password\":\"secret\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.token").value("hotel-jwt"))
                    .andExpect(jsonPath("$.data.role").value("FRONT_DESK"));
        }

        @Test
        @DisplayName("returns 400 when username is blank")
        void blankUsername() throws Exception {
            mockMvc.perform(post("/api/v1/hotels/{hotelId}/auth/login", HOTEL_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"\",\"password\":\"secret\"}"))
                    .andExpect(status().isBadRequest());

            then(hotelAuthUseCase).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("returns 422 when hotel is not active")
        void inactiveHotel() throws Exception {
            given(hotelAuthUseCase.login(argThat(cmd -> cmd.hotelId().equals(HOTEL_ID))))
                    .willThrow(new BusinessRuleViolationException("Hotel is not currently active"));

            mockMvc.perform(post("/api/v1/hotels/{hotelId}/auth/login", HOTEL_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"alice\",\"password\":\"secret\"}"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Hotel is not currently active"));
        }

        @Test
        @DisplayName("returns 422 when credentials are invalid")
        void badCredentials() throws Exception {
            given(hotelAuthUseCase.login(argThat(cmd -> cmd.hotelId().equals(HOTEL_ID))))
                    .willThrow(new BusinessRuleViolationException("Invalid username or password"));

            mockMvc.perform(post("/api/v1/hotels/{hotelId}/auth/login", HOTEL_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"alice\",\"password\":\"wrong\"}"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value("Invalid username or password"));
        }
    }
}
