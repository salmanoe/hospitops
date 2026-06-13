package id.co.hospitops.channel.domain.model;

import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.ReservationId;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Idempotency + audit record linking an external OTA booking to the HospitOps
 * reservation it produced. Keyed (per hotel) by the provider's stable
 * {@code externalBookingId}, so re-served revisions don't double-book.
 */
@Getter
public class ChannelInboundBooking {

    private final UUID id;
    private final HotelId hotelId;
    private final String externalBookingId;
    private ReservationId reservationId;
    private String lastRevisionId;
    private InboundStatus status;
    private final String otaName;
    private String otaReservationCode;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ChannelInboundBooking create(HotelId hotelId, String externalBookingId, String otaName) {
        LocalDateTime now = LocalDateTime.now();
        return new ChannelInboundBooking(UUID.randomUUID(), hotelId, externalBookingId, null, null,
                InboundStatus.CONFLICT, otaName, null, now, now);
    }

    public static ChannelInboundBooking reconstitute(UUID id, HotelId hotelId, String externalBookingId,
                                                     ReservationId reservationId, String lastRevisionId,
                                                     InboundStatus status, String otaName, String otaReservationCode,
                                                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new ChannelInboundBooking(id, hotelId, externalBookingId, reservationId, lastRevisionId,
                status, otaName, otaReservationCode, createdAt, updatedAt);
    }

    private ChannelInboundBooking(UUID id, HotelId hotelId, String externalBookingId, ReservationId reservationId,
                                  String lastRevisionId, InboundStatus status, String otaName,
                                  String otaReservationCode, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.hotelId = hotelId;
        this.externalBookingId = externalBookingId;
        this.reservationId = reservationId;
        this.lastRevisionId = lastRevisionId;
        this.status = status;
        this.otaName = otaName;
        this.otaReservationCode = otaReservationCode;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void markBooked(ReservationId reservationId, String revisionId, String otaReservationCode) {
        this.reservationId = reservationId;
        this.lastRevisionId = revisionId;
        this.otaReservationCode = otaReservationCode;
        this.status = InboundStatus.BOOKED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markCancelled(String revisionId) {
        this.lastRevisionId = revisionId;
        this.status = InboundStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markConflict(String revisionId) {
        this.reservationId = null;
        this.lastRevisionId = revisionId;
        this.status = InboundStatus.CONFLICT;
        this.updatedAt = LocalDateTime.now();
    }
}
