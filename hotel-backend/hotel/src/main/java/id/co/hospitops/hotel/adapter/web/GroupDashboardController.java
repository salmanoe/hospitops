package id.co.hospitops.hotel.adapter.web;

import id.co.hospitops.hotel.application.response.HotelSummaryResponse;
import id.co.hospitops.hotel.domain.port.in.GroupDashboardUseCase;
import id.co.hospitops.shared.GroupAdminPrincipal;
import id.co.hospitops.shared.web.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/group/dashboard")
@RequiredArgsConstructor
public class GroupDashboardController {

    private final GroupDashboardUseCase dashboardUseCase;

    /**
     * Returns pre-computed KPI summaries for all hotels in the group.
     * Reads exclusively from {@code hotel_summary} — no OLTP scans, safe under load.
     *
     * <p>Requires GROUP_ADMIN role (enforced in SecurityConfig).
     * The caller's group is derived exclusively from the authenticated principal —
     * a GROUP_ADMIN cannot query another group's dashboard.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<HotelSummaryResponse>>> getDashboard(
            @AuthenticationPrincipal GroupAdminPrincipal admin) {
        return ResponseEntity.ok(
                ApiResponse.ok(dashboardUseCase.getDashboard(admin.groupId())));
    }
}
