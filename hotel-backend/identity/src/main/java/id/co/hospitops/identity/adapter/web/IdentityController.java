package id.co.hospitops.identity.adapter.web;

import id.co.hospitops.identity.application.command.*;
import id.co.hospitops.identity.application.response.*;
import id.co.hospitops.identity.domain.model.Staff;
import id.co.hospitops.identity.domain.port.in.*;
import id.co.hospitops.shared.StaffId;
import id.co.hospitops.shared.web.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class IdentityController {

    private final AuthUseCase authUseCase;
    private final ManageStaffUseCase staffUseCase;

    @PostMapping("/auth/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginCommand command) {
        return ResponseEntity.ok(ApiResponse.ok(authUseCase.login(command)));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authHeader) {
        // R3-01 FIX: Use substring(7) to strip only the "Bearer " prefix.
        // String.replace() replaces ALL occurrences of the pattern — if a JWT
        // payload ever contained "Bearer " as a substring the token passed to
        // the blacklist would differ from the stored value, allowing reuse.
        // TokenService.extractFromHeader() applies the same safe logic.
        String token = authHeader.substring(7);
        authUseCase.logout(token);
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
    }

    @GetMapping("/auth/me")
    public ResponseEntity<ApiResponse<StaffResponse>> me(
            @AuthenticationPrincipal Staff staff) {
        return ResponseEntity.ok(ApiResponse.ok(StaffResponse.from(staff)));
    }

    @GetMapping("/staff")
    public ResponseEntity<ApiResponse<PageResult<StaffResponse>>> listStaff(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("fullName"));
        return ResponseEntity.ok(ApiResponse.ok(staffUseCase.findAll(pageable)));
    }

    @GetMapping("/staff/{id}")
    public ResponseEntity<ApiResponse<StaffResponse>> getStaff(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(staffUseCase.findById(StaffId.of(id))));
    }

    @PostMapping("/staff")
    @SuppressWarnings("JvmTaintAnalysis") // @Valid enforces Bean Validation on all string inputs; response is JSON, not HTML
    public ResponseEntity<ApiResponse<StaffResponse>> createStaff(
            @Valid @RequestBody CreateStaffCommand command) {
        StaffResponse created = staffUseCase.createStaff(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(created));
    }

    @PutMapping("/staff/{id}")
    public ResponseEntity<ApiResponse<StaffResponse>> updateStaff(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStaffCommand command) {
        return ResponseEntity.ok(
                ApiResponse.ok(staffUseCase.updateStaff(StaffId.of(id), command)));
    }

    @PatchMapping("/staff/{id}/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable UUID id,
            @Valid @RequestBody ChangePasswordCommand command) {
        staffUseCase.changePassword(StaffId.of(id), command);
        return ResponseEntity.ok(ApiResponse.ok("Password changed", null));
    }

    @PatchMapping("/staff/{id}/toggle")
    public ResponseEntity<ApiResponse<Void>> toggleActive(@PathVariable UUID id) {
        staffUseCase.toggleActive(StaffId.of(id));
        return ResponseEntity.ok(ApiResponse.ok("Status updated", null));
    }
}
