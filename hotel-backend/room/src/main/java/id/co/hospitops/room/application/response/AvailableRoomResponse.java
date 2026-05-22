package id.co.hospitops.room.application.response;

import id.co.hospitops.shared.RoomId;
import id.co.hospitops.shared.RoomTypeId;

import java.math.BigDecimal;

/**
 * Lightweight projection returned by the availability search.
 * Includes the effective nightly rate (after override resolution) so the UI
 * can display pricing without a second round-trip.
 */
public record AvailableRoomResponse(
        RoomId     id,
        String     roomNumber,
        int        floor,
        RoomTypeId roomTypeId,
        String     roomTypeName,
        int        capacity,
        BigDecimal ratePerNight
) {}
