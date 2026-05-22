package id.co.hospitops.identity.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.co.hospitops.identity.application.command.LoginCommand;
import id.co.hospitops.identity.application.response.LoginResponse;
import id.co.hospitops.identity.domain.model.StaffRole;
import id.co.hospitops.identity.domain.port.in.AuthUseCase;
import id.co.hospitops.identity.domain.port.in.ManageStaffUseCase;
import id.co.hospitops.shared.StaffId;
import id.co.hospitops.shared.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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
    @Autowired
    ObjectMapper objectMapper;
    @MockitoBean
    AuthUseCase authUseCase;
    @MockitoBean
    ManageStaffUseCase staffUseCase;

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {
        @Test
        @DisplayName("returns 200 with token for valid credentials")
        void returns200ForValidCredentials() throws Exception {
            LoginResponse mockResponse = new LoginResponse("jwt-token", "Bearer", 28800L,
                    StaffId.generate(), "Test User", "testuser", StaffRole.FRONT_DESK);
            given(authUseCase.login(any(LoginCommand.class))).willReturn(mockResponse);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginCommand("testuser", "password123"))))
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
                            .content(objectMapper.writeValueAsString(new LoginCommand("bad", "wrong"))))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Invalid username or password"));
        }
    }
}
