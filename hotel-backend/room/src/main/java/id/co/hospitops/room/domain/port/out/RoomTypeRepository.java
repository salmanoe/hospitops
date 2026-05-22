package id.co.hospitops.room.domain.port.out;

import id.co.hospitops.room.domain.model.RoomType;
import id.co.hospitops.shared.RoomTypeId;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface RoomTypeRepository {

    RoomType save(RoomType roomType);

    Optional<RoomType> findById(RoomTypeId id);

    boolean existsByName(String name);

    List<RoomType> findAll(Pageable pageable);

    long count();
}
