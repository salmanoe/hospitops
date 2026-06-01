package id.co.hospitops.hotel.domain.port.out;

import id.co.hospitops.hotel.domain.model.Hotel;
import id.co.hospitops.hotel.domain.model.HotelStatus;
import id.co.hospitops.shared.GroupId;
import id.co.hospitops.shared.HotelId;

import java.util.List;
import java.util.Optional;

public interface HotelRepository {
    Hotel save(Hotel hotel);

    Optional<Hotel> findById(HotelId id);

    List<Hotel> findByGroupId(GroupId groupId);

    List<Hotel> findAll();

    /**
     * Returns the hotel's current status without loading the full aggregate.
     * Use this for lightweight status checks (e.g. auth flows) to avoid the
     * hotel + checklist join on every request.
     */
    Optional<HotelStatus> findStatusById(HotelId id);

    /**
     * Returns the hotel's status and groupId without loading the full aggregate.
     * Used by cross-module adapters that need both values in a single query
     * (e.g. the /enter endpoint which checks both belongsToGroup and isActive).
     */
    Optional<HotelSnapshot> findSnapshotById(HotelId id);

    record HotelSnapshot(HotelStatus status, GroupId groupId, String name) {}
}
