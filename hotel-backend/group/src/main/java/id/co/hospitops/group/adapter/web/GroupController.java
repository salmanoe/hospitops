package id.co.hospitops.group.adapter.web;

import id.co.hospitops.group.adapter.web.request.SignupRequest;
import id.co.hospitops.group.application.command.SignupGroupCommand;
import id.co.hospitops.group.application.response.GroupResponse;
import id.co.hospitops.group.application.response.SignupResponse;
import id.co.hospitops.group.domain.port.in.ManageGroupUseCase;
import id.co.hospitops.shared.GroupId;
import id.co.hospitops.shared.web.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/group")
@RequiredArgsConstructor
public class GroupController {

    private final ManageGroupUseCase groupUseCase;

    /**
     * Public self-service signup. Creates a new hotel group and its first GROUP_ADMIN account.
     * The GROUP_ADMIN can log in once Phase 6 (auth endpoints) is implemented.
     */
    @PostMapping("/auth/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(
            @Valid @RequestBody SignupRequest req) {
        var cmd = new SignupGroupCommand(req.groupName(), req.adminEmail(), req.password());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(groupUseCase.signup(cmd)));
    }

    /**
     * Returns the group profile.
     * Requires GROUP_ADMIN role (enforced in SecurityConfig).
     *
     * TODO Phase 6: Extract groupId from JWT claim instead of path variable.
     */
    @GetMapping("/{groupId}/profile")
    public ResponseEntity<ApiResponse<GroupResponse>> getProfile(
            @PathVariable UUID groupId) {
        return ResponseEntity.ok(
                ApiResponse.ok(groupUseCase.findById(GroupId.of(groupId))));
    }
}
