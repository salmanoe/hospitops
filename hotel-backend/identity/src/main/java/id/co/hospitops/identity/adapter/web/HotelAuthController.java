package id.co.hospitops.identity.adapter.web;

import id.co.hospitops.identity.application.command.HotelLoginCommand;
import id.co.hospitops.identity.application.response.LoginResponse;
import id.co.hospitops.identity.domain.port.in.HotelAuthUseCase;
import id.co.hospitops.shared.web.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Hotel-aware staff login endpoint.
 *
 * <p>Exposes {@code POST /api/v1/hotels/{hotelId}/auth/login}, which validates
 * that the hotel is ACTIVE and that the staff account belongs to the requested hotel
 * before issuing a hotel-scoped JWT.
 *
 * <p>This endpoint is public (no JWT required) — see {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/v1/hotels/{hotelId}/auth")
@RequiredArgsConstructor
public class HotelAuthController {

    private final HotelAuthUseCase hotelAuthUseCase;

    /**
     * Authenticates a staff member against a specific hotel.
     *
     * <p>Returns HTTP 403 (via {@link id.co.hospitops.shared.exception.BusinessRuleViolationException})
     * if the hotel is not ACTIVE. Returns HTTP 401 for invalid credentials.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @PathVariable UUID hotelId,
            @Valid @RequestBody HotelLoginBody body) {

        var command = new HotelLoginCommand(hotelId, body.username(), body.password());
        return ResponseEntity.ok(ApiResponse.ok(hotelAuthUseCase.login(command)));
    }

    /**
     * Request body for the hotel-scoped login endpoint.
     * The hotelId is bound from the path — not from the body — to make the
     * hotel association explicit in the URL.
     */
    public record HotelLoginBody(
            @NotBlank String username,
            @NotBlank String password
    ) {
    }
}
