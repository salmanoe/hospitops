package id.co.hospitops.channel.application.response;

import id.co.hospitops.channel.domain.model.ChannelInboundBooking;
import id.co.hospitops.channel.domain.model.InboundStatus;
import id.co.hospitops.shared.ReservationId;

import java.time.LocalDateTime;

public record ChannelInboundBookingResponse(
        String externalBookingId,
        InboundStatus status,
        String otaName,
        String otaReservationCode,
        ReservationId reservationId,
        String lastRevisionId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ChannelInboundBookingResponse from(ChannelInboundBooking b) {
        return new ChannelInboundBookingResponse(
                b.getExternalBookingId(), b.getStatus(), b.getOtaName(), b.getOtaReservationCode(),
                b.getReservationId(), b.getLastRevisionId(), b.getCreatedAt(), b.getUpdatedAt());
    }
}
