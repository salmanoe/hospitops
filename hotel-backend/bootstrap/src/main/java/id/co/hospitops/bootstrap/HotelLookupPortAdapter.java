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
 * <p>Both {@code isActive} and {@code belongsToGroup} are called together by the
 * {@code /enter} endpoint. They share a single {@link HotelRepository#findSnapshotById}
 * call rather than loading the full Hotel aggregate twice.
 *
 * <p>Lives in {@code bootstrap} — the only module allowed to depend on both
 * {@code group} and {@code hotel} simultaneously.
 */
@Component
@RequiredArgsConstructor
public class HotelLookupPortAdapter implements HotelLookupPort {

    private final HotelRepository hotelRepository;

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
}
