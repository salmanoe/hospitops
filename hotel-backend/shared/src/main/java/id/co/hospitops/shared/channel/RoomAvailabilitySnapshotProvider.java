package id.co.hospitops.shared.channel;

import id.co.hospitops.shared.Money;
import id.co.hospitops.shared.RoomId;
import id.co.hospitops.shared.RoomTypeId;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Shared-kernel SPI that lets the {@code channel} module read room availability
 * and pricing without depending on the {@code room}/{@code reservation} modules.
 * Implemented by the {@code room} module, which already owns the cross-module
 * availability query (this is the {@code OccupancyPort} direction the room
 * adapter's R3-04 note anticipated).
 *
 * <p>Reads are resolved against the current {@code HotelContext} where the
 * underlying repositories require it, so call within a hotel-scoped flow.
 */
public interface RoomAvailabilitySnapshotProvider {

    /** The room type a room belongs to, if the room exists in the current hotel. */
    Optional<RoomTypeId> roomTypeOf(RoomId roomId);

    /**
     * Sellable units of a room type on a given night — physical inventory of that
     * type minus rooms held by an active (CONFIRMED/CHECKED_IN) reservation
     * covering the night. This is the availability to publish to OTAs.
     */
    int availableUnits(RoomTypeId roomTypeId, LocalDate night);

    /**
     * Rate to publish for a room type on a given night: an active seasonal
     * override if one applies, otherwise the room type's base price. Returns
     * {@link Money} so the currency (and thus minor-unit conversion) is known
     * at the channel/Channex boundary.
     */
    Money ratePerNight(RoomTypeId roomTypeId, LocalDate night);
}
