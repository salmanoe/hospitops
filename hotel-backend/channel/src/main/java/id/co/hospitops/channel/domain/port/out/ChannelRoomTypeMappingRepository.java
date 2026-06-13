package id.co.hospitops.channel.domain.port.out;

import id.co.hospitops.channel.domain.model.ChannelRoomTypeMapping;
import id.co.hospitops.shared.RoomTypeId;

import java.util.List;
import java.util.Optional;

/**
 * Persistence port for {@link ChannelRoomTypeMapping}. All reads are scoped
 * to the current {@code HotelContext} by the adapter.
 */
public interface ChannelRoomTypeMappingRepository {

    ChannelRoomTypeMapping save(ChannelRoomTypeMapping mapping);

    Optional<ChannelRoomTypeMapping> findByRoomTypeId(RoomTypeId roomTypeId);

    /** Reverse lookup by the provider's room-type id within the current hotel. */
    Optional<ChannelRoomTypeMapping> findByExternalRoomTypeId(String externalRoomTypeId);

    List<ChannelRoomTypeMapping> findAll();

    boolean existsByRoomTypeId(RoomTypeId roomTypeId);
}
