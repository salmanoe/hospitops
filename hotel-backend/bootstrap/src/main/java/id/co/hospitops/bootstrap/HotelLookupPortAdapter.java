package id.co.hospitops.bootstrap;

import id.co.hospitops.group.domain.port.out.HotelLookupPort;
import id.co.hospitops.hotel.domain.model.HotelStatus;
import id.co.hospitops.hotel.domain.port.out.HotelRepository;
import id.co.hospitops.shared.GroupId;
import id.co.hospitops.shared.HotelId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implements the {@code group} module's {@link HotelLookupPort} using the
 * {@code hotel} module's domain repository.
 *
 * <p>Lives in {@code bootstrap} — the only module allowed to depend on both
 * {@code group} and {@code hotel} simultaneously.
 *
 * <p>{@link #isActive} and {@link #belongsToGroup} each make an independent
 * {@link HotelRepository#findSnapshotById} call. Use {@link #verifyAccess} when
 * both checks are needed together (e.g. the {@code /enter} endpoint) to avoid
 * two round-trips for the same row.
 */
@Component
@RequiredArgsConstructor
public class HotelLookupPortAdapter implements HotelLookupPort {

    private final HotelRepository hotelRepository;

    @Override
    public String findHotelName(HotelId hotelId) {
        return hotelRepository.findSnapshotById(hotelId)
                .map(snapshot -> snapshot.name() != null ? snapshot.name() : "")
                .orElse("");
    }

    @Override
    public boolean isActive(HotelId hotelId) {
        return hotelRepository.findSnapshotById(hotelId)
                .map(snapshot -> snapshot.status() == HotelStatus.ACTIVE)
                .orElse(false);
    }

    @Override
    public boolean belongsToGroup(HotelId hotelId, GroupId groupId) {
        return hotelRepository.findSnapshotById(hotelId)
                .map(snapshot -> groupId.equals(snapshot.groupId()))
                .orElse(false);
    }

    /**
     * Loads the hotel snapshot once and evaluates both checks in a single query.
     */
    @Override
    public HotelAccessResult verifyAccess(HotelId hotelId, GroupId groupId) {
        return hotelRepository.findSnapshotById(hotelId)
                .map(snapshot -> {
                    if (!groupId.equals(snapshot.groupId())) {
                        return HotelAccessResult.NOT_FOUND_OR_WRONG_GROUP;
                    }
                    return snapshot.status() == HotelStatus.SUSPENDED
                            ? HotelAccessResult.SUSPENDED
                            : HotelAccessResult.ALLOWED; // ACTIVE or SETUP both permit entry
                })
                .orElse(HotelAccessResult.NOT_FOUND_OR_WRONG_GROUP);
    }
}
