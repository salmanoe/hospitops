package id.co.hospitops.bootstrap.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 *
 * <p>Uses a standalone MockMvc setup with a stub controller that throws
 * specific exceptions so each handler can be tested in isolation without
 * starting a full Spring context.
 *
 * <p>Key regression covered: {@link AccessDeniedException} thrown by
 * {@code @PreAuthorize} must NOT be caught by the {@code Exception} fallback
 * and mapped to 500 — it must be caught by its own handler and returned as 403.
 */
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new StubController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ── AccessDeniedException ───────────────────────────────────────────────

    @Test
    @DisplayName("AccessDeniedException → 403 with error body (regression: was 500)")
    void accessDenied_returns403() throws Exception {
        mockMvc.perform(get("/stub/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    // ── IllegalStateException ──────────────────────────────────────────────

    @Test
    @DisplayName("IllegalStateException → 422")
    void illegalState_returns422() throws Exception {
        mockMvc.perform(get("/stub/illegal-state"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("bad state"));
    }

    // ── Unhandled Exception ────────────────────────────────────────────────

    @Test
    @DisplayName("unhandled Exception → 500")
    void unhandledException_returns500() throws Exception {
        mockMvc.perform(get("/stub/generic"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── Stub controller ───────────────────────────────────────────────────

    @RestController
    static class StubController {

        @GetMapping("/stub/access-denied")
        public void accessDenied() {
            throw new AccessDeniedException("not allowed");
        }

        @GetMapping("/stub/illegal-state")
        public void illegalState() {
            throw new IllegalStateException("bad state");
        }

        @GetMapping("/stub/generic")
        public void generic() throws Exception {
            throw new Exception("unexpected");
        }
    }
}
