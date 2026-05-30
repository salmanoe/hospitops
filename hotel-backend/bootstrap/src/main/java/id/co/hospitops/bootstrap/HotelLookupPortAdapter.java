package id.co.hospitops.bootstrap;

import id.co.hospitops.group.domain.port.out.HotelLookupPort;
import id.co.hospitops.hotel.domain.model.Hotel;
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
 */
@Component
@RequiredArgsConstructor
public class HotelLookupPortAdapter implements HotelLookupPort {

    private final HotelRepository hotelRepository;

    @Override
    public boolean isActive(HotelId hotelId) {
        return hotelRepository.findById(hotelId)
                .map(hotel -> hotel.getStatus() == HotelStatus.ACTIVE)
                .orElse(false);
    }

    @Override
    public boolean belongsToGroup(HotelId hotelId, GroupId groupId) {
        return hotelRepository.findById(hotelId)
                .map(Hotel::getGroupId)
                .map(groupId::equals)
                .orElse(false);
    }
}
