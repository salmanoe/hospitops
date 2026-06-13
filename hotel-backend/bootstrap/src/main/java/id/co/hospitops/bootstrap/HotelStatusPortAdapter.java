package id.co.hospitops.bootstrap;

import id.co.hospitops.hotel.domain.model.HotelStatus;
import id.co.hospitops.hotel.domain.port.out.HotelRepository;
import id.co.hospitops.identity.domain.port.out.HotelStatusPort;
import id.co.hospitops.shared.HotelId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implements the {@code identity} module's {@link HotelStatusPort} using the
 * {@code hotel} module's domain repository.
 *
 * <p>Uses a focused status-only query rather than loading the full Hotel aggregate —
 * this runs on every staff login and must stay lightweight.
 *
 * <p>Lives in {@code bootstrap} — the only module that is allowed to depend on both
 * {@code identity} and {@code hotel} simultaneously.
 */
@Component
@RequiredArgsConstructor
public class HotelStatusPortAdapter implements HotelStatusPort {

    private final HotelRepository hotelRepository;

    @Override
    public boolean isActive(HotelId hotelId) {
        return hotelRepository.findStatusById(hotelId)
                .map(status -> status == HotelStatus.ACTIVE)
                .orElse(false);
    }
}
