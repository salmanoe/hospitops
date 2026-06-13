package id.co.hospitops.group.adapter.web;

import id.co.hospitops.group.application.command.GroupLoginCommand;
import id.co.hospitops.group.application.response.GroupLoginResponse;
import id.co.hospitops.group.domain.port.in.GroupAuthUseCase;
import id.co.hospitops.shared.GroupAdminPrincipal;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.web.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * GROUP_ADMIN authentication endpoints.
 *
 * <ul>
 *   <li>{@code POST /api/v1/group/auth/login} — public, issues a group-scoped JWT</li>
 *   <li>{@code POST /api/v1/group/hotels/{hotelId}/enter} — GROUP_ADMIN only,
 *       exchanges the group token for a hotel-scoped JWT</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/group")
@RequiredArgsConstructor
public class GroupAuthController {

    private final GroupAuthUseCase groupAuthUseCase;

    /**
     * Authenticates a GROUP_ADMIN and returns a group-scoped JWT.
     * The returned token grants access to {@code /api/v1/group/**} endpoints only.
     * Call {@code /enter} to obtain a hotel-scoped token for hotel operations.
     */
    @PostMapping("/auth/login")
    public ResponseEntity<ApiResponse<GroupLoginResponse>> login(
            @Valid @RequestBody GroupLoginBody body) {

        var command = new GroupLoginCommand(body.email(), body.password());
        return ResponseEntity.ok(ApiResponse.ok(groupAuthUseCase.login(command)));
    }

    /**
     * Exchanges the caller's group-scoped token for a hotel-scoped GROUP_ADMIN token.
     *
     * <p>The hotel must belong to the caller's group and be in {@code ACTIVE} status.
     * The returned token carries the same expiry as the originating group token.
     *
     * <p>Requires: {@code Authorization: Bearer <group-scoped-token>}
     */
    @PostMapping("/hotels/{hotelId}/enter")
    public ResponseEntity<ApiResponse<GroupLoginResponse>> enterHotel(
            @PathVariable UUID hotelId,
            @RequestHeader("Authorization") String authHeader,
            @AuthenticationPrincipal GroupAdminPrincipal admin) {

        // Strip "Bearer " prefix — header has already been verified by JwtAuthFilter
        String groupToken = authHeader.substring(7);
        GroupLoginResponse response = groupAuthUseCase.enterHotel(
                admin, HotelId.of(hotelId), groupToken);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    public record GroupLoginBody(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {
    }
}
