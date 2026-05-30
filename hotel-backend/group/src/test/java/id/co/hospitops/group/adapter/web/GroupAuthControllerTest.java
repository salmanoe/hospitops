package id.co.hospitops.group.adapter.web;

import id.co.hospitops.group.application.response.GroupLoginResponse;
import id.co.hospitops.group.domain.port.in.GroupAuthUseCase;
import id.co.hospitops.group.domain.port.in.ManageGroupUseCase;
import id.co.hospitops.shared.GroupAdminId;
import id.co.hospitops.shared.GroupAdminPrincipal;
import id.co.hospitops.shared.GroupId;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GroupAuthController.class)
@DisplayName("GroupAuthController")
class GroupAuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    GroupAuthUseCase groupAuthUseCase;

    // GroupController is in the same adapter.web scan scope — mock its dependency.
    @MockitoBean
    ManageGroupUseCase manageGroupUseCase;

    private static final GroupId GROUP_ID = GroupId.generate();
    private static final GroupAdminId ADMIN_ID = GroupAdminId.generate();
    private static final String EMAIL = "admin@acme.com";
    private static final UUID HOTEL_UUID = UUID.randomUUID();
    private static final HotelId HOTEL_ID = HotelId.of(HOTEL_UUID);

    private GroupLoginResponse groupScopedResponse() {
        return GroupLoginResponse.groupScoped("group-token", 28_800L, ADMIN_ID, GROUP_ID, EMAIL);
    }

    private GroupLoginResponse hotelScopedResponse() {
        return GroupLoginResponse.hotelScoped("hotel-token", 28_800L, ADMIN_ID, GROUP_ID, EMAIL, HOTEL_ID);
    }

    /**
     * Builds a Spring Security authentication for GROUP_ADMIN (no hotel scope).
     */
    private UsernamePasswordAuthenticationToken groupAdminAuth() {
        var principal = new GroupAdminPrincipal(ADMIN_ID, GROUP_ID, EMAIL, null);
        return new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_GROUP_ADMIN")));
    }

    @Nested
    @DisplayName("POST /api/v1/group/auth/login")
    class Login {

        @Test
        @DisplayName("returns 200 with group-scoped token on valid credentials")
        void success() throws Exception {
            given(groupAuthUseCase.login(argThat(cmd -> EMAIL.equals(cmd.email()))))
                    .willReturn(groupScopedResponse());

            mockMvc.perform(post("/api/v1/group/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"admin@acme.com\",\"password\":\"pass123\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value("group-token"))
                    .andExpect(jsonPath("$.data.hotelId").doesNotExist());
        }

        @Test
        @DisplayName("returns 400 when email is blank")
        void blankEmail() throws Exception {
            mockMvc.perform(post("/api/v1/group/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"\",\"password\":\"pass123\"}"))
                    .andExpect(status().isBadRequest());

            then(groupAuthUseCase).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("returns 422 on invalid credentials")
        void badCredentials() throws Exception {
            given(groupAuthUseCase.login(any()))
                    .willThrow(new BusinessRuleViolationException("Invalid email or password"));

            mockMvc.perform(post("/api/v1/group/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"admin@acme.com\",\"password\":\"wrong\"}"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value("Invalid email or password"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/group/hotels/{hotelId}/enter")
    class EnterHotel {

        @Test
        @DisplayName("returns 200 with hotel-scoped token when hotel is valid")
        void success() throws Exception {
            given(groupAuthUseCase.enterHotel(any(), argThat(h -> h.value().equals(HOTEL_UUID)), any()))
                    .willReturn(hotelScopedResponse());

            mockMvc.perform(post("/api/v1/group/hotels/{hotelId}/enter", HOTEL_UUID)
                            .header("Authorization", "Bearer group-token-here")
                            .contentType(MediaType.APPLICATION_JSON)
                            .with(authentication(groupAdminAuth())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").value("hotel-token"))
                    .andExpect(jsonPath("$.data.hotelId").isNotEmpty());
        }

        @Test
        @DisplayName("returns 422 when hotel does not belong to group")
        void wrongGroup() throws Exception {
            given(groupAuthUseCase.enterHotel(any(), any(), any()))
                    .willThrow(new BusinessRuleViolationException("Hotel does not belong to your group"));

            mockMvc.perform(post("/api/v1/group/hotels/{hotelId}/enter", HOTEL_UUID)
                            .header("Authorization", "Bearer group-token-here")
                            .contentType(MediaType.APPLICATION_JSON)
                            .with(authentication(groupAdminAuth())))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value("Hotel does not belong to your group"));
        }
    }
}
