package id.co.hospitops.identity.adapter.web;

import id.co.hospitops.identity.application.response.LoginResponse;
import id.co.hospitops.identity.domain.model.StaffRole;
import id.co.hospitops.identity.domain.port.in.AuthUseCase;
import id.co.hospitops.identity.domain.port.in.ManageStaffUseCase;
import id.co.hospitops.identity.domain.port.out.StaffRepository;
import id.co.hospitops.identity.domain.port.out.TokenBlacklist;
import id.co.hospitops.identity.infrastructure.security.JwtUtil;
import id.co.hospitops.shared.StaffId;
import id.co.hospitops.shared.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IdentityController.class)
@DisplayName("IdentityController — /api/v1/auth")
class IdentityControllerTest {

    @Autowired
    MockMvc mockMvc;

    // Spring Boot 4.0 uses Jackson 3.x (tools.jackson), so ObjectMapper from
    // com.fasterxml.jackson is not available as a Spring bean in the test slice.
    // Request bodies are provided as inline JSON strings instead.

    @MockitoBean
    AuthUseCase authUseCase;
    @MockitoBean
    ManageStaffUseCase staffUseCase;

    // JwtAuthFilter (@Component Filter) is included in the @WebMvcTest slice;
    // its dependencies must be mocked so the context loads.
    @MockitoBean
    JwtUtil jwtUtil;
    @MockitoBean
    TokenBlacklist tokenBlacklist;
    @MockitoBean
    StaffRepository staffRepository;

    /** Builds a LoginResponse with both access and refresh token fields populated. */
    private static LoginResponse mockLoginResponse() {
        return new LoginResponse(
                "jwt-token", "Bearer", 28800L,
                "refresh-uuid", 604800L,
                StaffId.generate(), "Test User", "testuser", StaffRole.FRONT_DESK);
    }

    // ── POST /api/v1/auth/login ──────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {

        @Test
        @DisplayName("returns 200 with access and refresh tokens for valid credentials")
        void returns200ForValidCredentials() throws Exception {
            given(authUseCase.login(any())).willReturn(mockLoginResponse());

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"testuser\",\"password\":\"password123\"}"))
                    .andExpect(status().is(200))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.token").value("jwt-token"))
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.data.refreshToken").value("refresh-uuid"))
                    .andExpect(jsonPath("$.data.refreshExpiresIn").value(604800))
                    .andExpect(jsonPath("$.data.role").value("FRONT_DESK"));
        }

        @Test
        @DisplayName("returns 400 when username is blank")
        void returns400ForBlankUsername() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"username\": \"\", \"password\": \"pass\" }"))
                    .andExpect(status().is(400))
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("returns 422 for invalid credentials")
        void returns422ForInvalidCredentials() throws Exception {
            given(authUseCase.login(any()))
                    .willThrow(new BusinessRuleViolationException("Invalid username or password"));

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"bad\",\"password\":\"wrong\"}"))
                    .andExpect(status().is(422))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Invalid username or password"));
        }
    }

    // ── POST /api/v1/auth/refresh ────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class Refresh {

        @Test
        @DisplayName("returns 200 with new access + refresh tokens for a valid refresh token")
        void returns200ForValidRefreshToken() throws Exception {
            LoginResponse rotated = new LoginResponse(
                    "new-jwt", "Bearer", 28800L,
                    "new-refresh-uuid", 604800L,
                    StaffId.generate(), "Test User", "testuser", StaffRole.FRONT_DESK);
            given(authUseCase.refresh("valid-refresh-uuid")).willReturn(rotated);

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"valid-refresh-uuid\"}"))
                    .andExpect(status().is(200))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.token").value("new-jwt"))
                    .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-uuid"));
        }

        @Test
        @DisplayName("returns 400 when refreshToken is blank (Bean Validation)")
        void returns400ForBlankRefreshToken() throws Exception {
            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"\"}"))
                    .andExpect(status().is(400))
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("returns 422 for an unknown or expired refresh token")
        void returns422ForExpiredRefreshToken() throws Exception {
            given(authUseCase.refresh(any()))
                    .willThrow(new BusinessRuleViolationException("Refresh token is invalid or expired"));

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"stale-uuid\"}"))
                    .andExpect(status().is(422))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Refresh token is invalid or expired"));
        }

        @Test
        @DisplayName("returns 422 for an inactive staff member owning the refresh token")
        void returns422ForInactiveStaff() throws Exception {
            given(authUseCase.refresh(any()))
                    .willThrow(new BusinessRuleViolationException("Account is deactivated"));

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"valid-but-deactivated\"}"))
                    .andExpect(status().is(422))
                    .andExpect(jsonPath("$.success").value(false));
        }
    }
}
