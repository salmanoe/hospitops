package id.co.hospitops.group.adapter.web;

import id.co.hospitops.group.application.response.GroupResponse;
import id.co.hospitops.group.domain.port.in.GroupAuthUseCase;
import id.co.hospitops.group.domain.port.in.ManageGroupUseCase;
import id.co.hospitops.shared.GroupAdminId;
import id.co.hospitops.shared.GroupAdminPrincipal;
import id.co.hospitops.shared.GroupId;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GroupController.class)
@DisplayName("GroupController")
class GroupControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ManageGroupUseCase groupUseCase;

    // Sibling controller in the same scan scope — mock its dependency.
    @MockitoBean
    GroupAuthUseCase groupAuthUseCase;

    private static final UUID GROUP_UUID = UUID.randomUUID();
    private static final UUID ADMIN_UUID = UUID.randomUUID();

    /**
     * Populates {@code SecurityContextHolder} with a GROUP_ADMIN principal before each test.
     * {@link AuthenticationPrincipalArgumentResolver} reads directly from
     * {@code SecurityContextHolder}, so no security filter chain is required.
     */
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

    private GroupResponse groupResponse() {
        return new GroupResponse(GROUP_UUID, "Acme Hotels", "admin@hospitops.local",
                LocalDateTime.now());
    }

    // ── POST /api/v1/group/auth/signup ────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/group/auth/signup")
    class Signup {

        @Test
        @DisplayName("returns 201 on valid signup")
        void success() throws Exception {
            mockMvc.perform(post("/api/v1/group/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"groupName":"Acme Hotels",
                                     "adminEmail":"admin@acme.com",
                                     "password":"secret123"}
                                    """))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("returns 400 when groupName is blank")
        void blankGroupName() throws Exception {
            mockMvc.perform(post("/api/v1/group/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"groupName":"",
                                     "adminEmail":"admin@acme.com",
                                     "password":"secret123"}
                                    """))
                    .andExpect(status().isBadRequest());

            then(groupUseCase).shouldHaveNoInteractions();
        }
    }

    // ── GET /api/v1/group/profile ─────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/group/profile")
    class GetProfile {

        @Test
        @DisplayName("returns 200 using groupId from the authenticated principal")
        void success() throws Exception {
            given(groupUseCase.findById(GroupId.of(GROUP_UUID))).willReturn(groupResponse());

            mockMvc.perform(get("/api/v1/group/profile")
        )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(GROUP_UUID.toString()))
                    .andExpect(jsonPath("$.data.name").value("Acme Hotels"));

            then(groupUseCase).should().findById(GroupId.of(GROUP_UUID));
        }

        @Test
        @DisplayName("returns 404 when the group does not exist")
        void notFound() throws Exception {
            given(groupUseCase.findById(GroupId.of(GROUP_UUID)))
                    .willThrow(new ResourceNotFoundException("Group", GROUP_UUID));

            mockMvc.perform(get("/api/v1/group/profile")
        )
                    .andExpect(status().isNotFound());
        }
    }
}
