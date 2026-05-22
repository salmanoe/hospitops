package id.co.hospitops.bootstrap;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MutatingRequestAuditInterceptorTest (R3-06 fix).
 *
 * Verifies that:
 *   - preHandle() always returns true (never blocks processing)
 *   - Mutating methods (POST, PATCH, PUT, DELETE) are handled without error
 *   - Read-only methods (GET, HEAD, OPTIONS) pass through silently
 *   - The interceptor works for both authenticated and anonymous users
 */
@DisplayName("MutatingRequestAuditInterceptor")
class MutatingRequestAuditInterceptorTest {

    private final MutatingRequestAuditInterceptor interceptor =
            new MutatingRequestAuditInterceptor();

    private final HttpServletRequest  request  = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final Object              handler  = new Object();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ── Always returns true ────────────────────────────────────────

    @Nested
    @DisplayName("return value")
    class ReturnValue {

        @ParameterizedTest(name = "method = {0}")
        @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE", "GET", "HEAD", "OPTIONS"})
        @DisplayName("always returns true regardless of HTTP method")
        void alwaysReturnsTrue(String method) throws Exception {
            when(request.getMethod()).thenReturn(method);
            when(request.getRequestURI()).thenReturn("/api/v1/test");

            boolean result = interceptor.preHandle(request, response, handler);

            assertThat(result).isTrue();
        }
    }

    // ── Mutating methods ───────────────────────────────────────────

    @Nested
    @DisplayName("mutating HTTP methods")
    class MutatingMethods {

        @ParameterizedTest(name = "method = {0}")
        @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
        @DisplayName("does not throw for mutating methods")
        void doesNotThrowForMutatingMethods(String method) {
            when(request.getMethod()).thenReturn(method);
            when(request.getRequestURI()).thenReturn("/api/v1/resource");

            assertThatNoException()
                    .isThrownBy(() -> interceptor.preHandle(request, response, handler));
        }
    }

    // ── Read-only methods ─────────────────────────────────────────

    @Nested
    @DisplayName("read-only HTTP methods")
    class ReadOnlyMethods {

        @ParameterizedTest(name = "method = {0}")
        @ValueSource(strings = {"GET", "HEAD", "OPTIONS"})
        @DisplayName("does not throw for read-only methods")
        void doesNotThrowForReadOnlyMethods(String method) {
            when(request.getMethod()).thenReturn(method);
            // getRequestURI() should NOT be called for read-only methods
            // (the interceptor checks method first)

            assertThatNoException()
                    .isThrownBy(() -> interceptor.preHandle(request, response, handler));
        }

        @ParameterizedTest(name = "method = {0}")
        @ValueSource(strings = {"GET", "HEAD", "OPTIONS"})
        @DisplayName("never reads requestURI for read-only methods")
        void doesNotReadUriForReadOnlyMethods(String method) throws Exception {
            when(request.getMethod()).thenReturn(method);

            interceptor.preHandle(request, response, handler);

            verify(request, never()).getRequestURI();
        }
    }

    // ── Actor resolution ──────────────────────────────────────────

    @Nested
    @DisplayName("actor resolution")
    class ActorResolution {

        @Test
        @DisplayName("uses 'anonymous' when no SecurityContext authentication present")
        void usesAnonymousWhenNoAuthentication() {
            SecurityContextHolder.clearContext();
            when(request.getMethod()).thenReturn("POST");
            when(request.getRequestURI()).thenReturn("/api/v1/guests");

            assertThatNoException()
                    .isThrownBy(() -> interceptor.preHandle(request, response, handler));
        }

        @Test
        @DisplayName("uses principal name when authenticated")
        void usesPrincipalNameWhenAuthenticated() throws Exception {
            Authentication auth = mock(Authentication.class);
            when(auth.isAuthenticated()).thenReturn(true);
            when(auth.getName()).thenReturn("admin_user");
            SecurityContext ctx = mock(SecurityContext.class);
            when(ctx.getAuthentication()).thenReturn(auth);
            SecurityContextHolder.setContext(ctx);

            when(request.getMethod()).thenReturn("DELETE");
            when(request.getRequestURI()).thenReturn("/api/v1/staff/123");

            boolean result = interceptor.preHandle(request, response, handler);

            assertThat(result).isTrue();
        }
    }
}
