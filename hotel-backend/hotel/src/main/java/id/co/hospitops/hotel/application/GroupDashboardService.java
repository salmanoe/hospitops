package id.co.hospitops.hotel.application;

import id.co.hospitops.hotel.application.response.HotelSummaryResponse;
import id.co.hospitops.hotel.domain.port.in.GroupDashboardUseCase;
import id.co.hospitops.hotel.domain.port.out.HotelSummaryRepository;
import id.co.hospitops.shared.GroupId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GroupDashboardService implements GroupDashboardUseCase {

    private final HotelSummaryRepository summaryRepo;

    @Override
    public List<HotelSummaryResponse> getDashboard(GroupId groupId) {
        return summaryRepo.findByGroupId(groupId)
                .stream()
                .map(HotelSummaryResponse::from)
                .toList();
    }
}
