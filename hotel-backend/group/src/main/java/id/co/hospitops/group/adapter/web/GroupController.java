package id.co.hospitops.group.adapter.web;

import id.co.hospitops.group.adapter.web.request.SignupRequest;
import id.co.hospitops.group.application.command.SignupGroupCommand;
import id.co.hospitops.group.application.response.GroupResponse;
import id.co.hospitops.group.application.response.SignupResponse;
import id.co.hospitops.group.domain.port.in.ManageGroupUseCase;
import id.co.hospitops.shared.GroupAdminPrincipal;
import id.co.hospitops.shared.web.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/group")
@RequiredArgsConstructor
public class GroupController {

    private final ManageGroupUseCase groupUseCase;

    /**
     * Public self-service signup. Creates a new hotel group and its first GROUP_ADMIN account.
     */
    @PostMapping("/auth/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(
            @Valid @RequestBody SignupRequest req) {
        var cmd = new SignupGroupCommand(req.groupName(), req.adminEmail(), req.password());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(groupUseCase.signup(cmd)));
    }

    /**
     * Returns the authenticated GROUP_ADMIN's own group profile.
     * Requires GROUP_ADMIN role (enforced in SecurityConfig).
     * The groupId is derived from the JWT claim — a caller cannot query another group.
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<GroupResponse>> getProfile(
            @AuthenticationPrincipal GroupAdminPrincipal admin) {
        return ResponseEntity.ok(
                ApiResponse.ok(groupUseCase.findById(admin.groupId())));
    }
}
