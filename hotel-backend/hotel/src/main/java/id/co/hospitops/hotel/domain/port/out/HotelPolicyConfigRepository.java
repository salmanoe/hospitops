package id.co.hospitops.hotel.domain.port.out;

import id.co.hospitops.hotel.domain.model.PolicyConfig;
import id.co.hospitops.shared.HotelId;

import java.util.Optional;

public interface HotelPolicyConfigRepository {

    PolicyConfig save(PolicyConfig config);

    Optional<PolicyConfig> findByHotelId(HotelId hotelId);

    boolean existsByHotelId(HotelId hotelId);
}
