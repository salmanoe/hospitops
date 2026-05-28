package id.co.hospitops.identity.adapter.web;

import id.co.hospitops.identity.application.command.LogoutCommand;
import id.co.hospitops.identity.domain.port.in.AuthUseCase;
import id.co.hospitops.identity.domain.port.in.ManageStaffUseCase;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for IdentityController.logout() — R3-01 fix.
 *
 * The fix changed authHeader.replace("Bearer ", "") to authHeader.substring(7).
 * String.replace() replaced every occurrence of "Bearer " in the string; if a
 * JWT payload contained "Bearer " as a substring the token passed to the
 * blacklist would differ from the stored value, allowing reuse of a
 * logged-out token.
 *
 * These tests verify:
 *   - The bare token (without "Bearer " prefix) is passed to AuthUseCase.logout()
 *   - A token whose payload happens to contain "Bearer " is passed unchanged
 *     (replace() would have corrupted it; substring(7) is safe)
 *   - Standard and edge-case header values are handled correctly
 *   - The optional refresh token from the request body is forwarded correctly
 */
@DisplayName("IdentityController.logout() — R3-01 token extraction")
@ExtendWith(MockitoExtension.class)
class IdentityControllerLogoutTest {

    @Mock AuthUseCase        authUseCase;
    @Mock ManageStaffUseCase staffUseCase;

    @InjectMocks IdentityController controller;

    // ── Correct token extraction ────────────────────────────────────

    @Nested
    @DisplayName("token extraction from Authorization header")
    class TokenExtraction {

        @Test
        @DisplayName("strips 'Bearer ' prefix and passes bare token to AuthUseCase")
        void stripsBearerPrefixCorrectly() {
            controller.logout("Bearer eyJhbGciOiJIUzI1NiJ9.payload.sig", null);

            verify(authUseCase).logout("eyJhbGciOiJIUzI1NiJ9.payload.sig", null);
        }

        @Test
        @DisplayName("token with 'Bearer ' in payload is NOT corrupted by substring(7)")
        void payloadContainingBearerIsPreserved() {
            // replace("Bearer ", "") would strip both occurrences and return a mangled token.
            // substring(7) only removes the first 7 characters, preserving the rest exactly.
            String header        = "Bearer eyBhbGc.Bearer .sig";
            String expectedToken = "eyBhbGc.Bearer .sig"; // everything after the first 7 chars

            controller.logout(header, null);

            verify(authUseCase).logout(expectedToken, null);
        }

        @Test
        @DisplayName("minimal single-character token after prefix is passed correctly")
        void minimalTokenAfterPrefix() {
            controller.logout("Bearer x", null);

            verify(authUseCase).logout("x", null);
        }

        @Test
        @DisplayName("authUseCase.logout() is called exactly once per request")
        void calledExactlyOnce() {
            controller.logout("Bearer some-token", null);

            verify(authUseCase, times(1)).logout(anyString(), any());
            verifyNoMoreInteractions(authUseCase);
        }
    }

    // ── Refresh token forwarding ────────────────────────────────────

    @Nested
    @DisplayName("refresh token forwarding from request body")
    class RefreshTokenForwarding {

        @Test
        @DisplayName("passes refresh token to AuthUseCase when LogoutCommand is present")
        void forwardsRefreshTokenWhenPresent() {
            controller.logout("Bearer access-jwt", new LogoutCommand("my-refresh-uuid"));

            verify(authUseCase).logout("access-jwt", "my-refresh-uuid");
        }

        @Test
        @DisplayName("passes null refresh token when LogoutCommand body is absent")
        void passesNullWhenCommandIsAbsent() {
            controller.logout("Bearer access-jwt", null);

            verify(authUseCase).logout("access-jwt", null);
        }
    }

    // ── Pre-fix regression guard ────────────────────────────────────

    @Nested
    @DisplayName("regression: replace() vs substring(7)")
    class ReplaceRegression {

        @Test
        @DisplayName("replace() would return empty string for 'Bearer Bearer ' — substring(7) returns 'Bearer '")
        void demonstratesReplaceBug() {
            // This header is pathological but proves the point:
            // replace("Bearer ", "") → ""       (both occurrences stripped)
            // substring(7)          → "Bearer " (correct — strip prefix only)
            String header        = "Bearer Bearer ";
            String expectedToken = "Bearer ";

            controller.logout(header, null);

            // If the old replace() logic were used, authUseCase.logout("", null) would be called.
            // With substring(7), authUseCase.logout("Bearer ", null) is called — the correct token.
            verify(authUseCase).logout(expectedToken, null);
        }
    }
}
