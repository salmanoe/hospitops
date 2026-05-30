package id.co.hospitops.hotel.domain.port.out;

import id.co.hospitops.hotel.domain.model.Hotel;
import id.co.hospitops.shared.GroupId;
import id.co.hospitops.shared.HotelId;

import java.util.List;
import java.util.Optional;

public interface HotelRepository {
    Hotel save(Hotel hotel);

    Optional<Hotel> findById(HotelId id);

    List<Hotel> findByGroupId(GroupId groupId);

    List<Hotel> findAll();
}
