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

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {
        @Test
        @DisplayName("returns 200 with token for valid credentials")
        void returns200ForValidCredentials() throws Exception {
            LoginResponse mockResponse = new LoginResponse("jwt-token", "Bearer", 28800L,
                    StaffId.generate(), "Test User", "testuser", StaffRole.FRONT_DESK);
            given(authUseCase.login(any())).willReturn(mockResponse);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"testuser\",\"password\":\"password123\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.token").value("jwt-token"))
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.data.role").value("FRONT_DESK"));
        }

        @Test
        @DisplayName("returns 400 when username is blank")
        void returns400ForBlankUsername() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"username\": \"\", \"password\": \"pass\" }"))
                    .andExpect(status().isBadRequest())
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
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Invalid username or password"));
        }
    }
}
