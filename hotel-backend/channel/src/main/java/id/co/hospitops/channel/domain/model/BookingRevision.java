package id.co.hospitops.channel.domain.model;

import java.time.LocalDate;
import java.util.List;

/**
 * An inbound booking revision pulled from the provider's revisions feed.
 * {@code bookingId} is stable across revisions of the same booking;
 * {@code revisionId} identifies this specific revision (used for ack).
 *
 * <p>A booking can span multiple room segments; the first cut assigns one local
 * room per segment.
 */
public record BookingRevision(
        String revisionId,
        String bookingId,
        RevisionStatus status,
        String externalPropertyId,
        String otaName,
        String otaReservationCode,
        String guestFullName,
        String guestEmail,
        String guestPhone,
        String guestNationality,
        List<RoomSegment> rooms) {

    public record RoomSegment(
            String externalRoomTypeId,
            String externalRatePlanId,
            LocalDate checkIn,
            LocalDate checkOut,
            int adults,
            int children) {
    }
}
