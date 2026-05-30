package id.co.hospitops.hotel.domain.port.in;

import id.co.hospitops.hotel.application.response.HotelSummaryResponse;
import id.co.hospitops.shared.GroupId;

import java.util.List;

public interface GroupDashboardUseCase {

    /**
     * Returns pre-computed KPI summaries for every hotel in the given group.
     * Reads from {@code hotel_summary} — no OLTP table scans.
     */
    List<HotelSummaryResponse> getDashboard(GroupId groupId);
}
