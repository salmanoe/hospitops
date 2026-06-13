package id.co.hospitops.hotel.domain.port.out;

import id.co.hospitops.hotel.domain.model.HotelSummary;
import id.co.hospitops.shared.GroupId;
import id.co.hospitops.shared.HotelId;

import java.util.List;
import java.util.Optional;

public interface HotelSummaryRepository {

    HotelSummary save(HotelSummary summary);

    Optional<HotelSummary> findByHotelId(HotelId hotelId);

    /**
     * Returns summaries for all hotels belonging to the given group.
     */
    List<HotelSummary> findByGroupId(GroupId groupId);
}
