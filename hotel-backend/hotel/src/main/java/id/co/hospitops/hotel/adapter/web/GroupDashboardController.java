package id.co.hospitops.hotel.adapter.web;

import id.co.hospitops.hotel.application.response.HotelSummaryResponse;
import id.co.hospitops.hotel.domain.port.in.GroupDashboardUseCase;
import id.co.hospitops.shared.GroupId;
import id.co.hospitops.shared.web.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

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
     * <p>
     * TODO Phase 6 follow-up: extract groupId from JWT claim, remove query parameter.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<HotelSummaryResponse>>> getDashboard(
            @RequestParam UUID groupId) {
        return ResponseEntity.ok(
                ApiResponse.ok(dashboardUseCase.getDashboard(GroupId.of(groupId))));
    }
}
