package id.co.hospitops.channel.infrastructure.persistence;

import id.co.hospitops.channel.domain.model.ChannelInboundBooking;
import id.co.hospitops.channel.domain.port.out.ChannelInboundBookingRepository;
import id.co.hospitops.channel.infrastructure.persistence.entity.ChannelInboundBookingJpaEntity;
import id.co.hospitops.shared.HotelContext;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.ReservationId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ChannelInboundBookingRepositoryImpl implements ChannelInboundBookingRepository {

    private final ChannelInboundBookingJpaRepository jpa;

    @Override
    public ChannelInboundBooking save(ChannelInboundBooking booking) {
        ChannelInboundBookingJpaEntity entity = jpa.findById(booking.getId())
                .map(existing -> {
                    existing.setReservationId(booking.getReservationId() == null
                            ? null : booking.getReservationId().value());
                    existing.setLastRevisionId(booking.getLastRevisionId());
                    existing.setStatus(booking.getStatus());
                    existing.setOtaReservationCode(booking.getOtaReservationCode());
                    return existing;
                })
                .orElseGet(() -> toJpa(booking));
        return toDomain(jpa.save(entity));
    }

    @Override
    public Optional<ChannelInboundBooking> findByExternalBookingId(String externalBookingId) {
        return jpa.findByHotelIdAndExternalBookingId(HotelContext.current().value(), externalBookingId)
                .map(this::toDomain);
    }

    @Override
    public List<ChannelInboundBooking> findRecentForCurrentHotel(int limit) {
        return jpa.findByHotelIdOrderByUpdatedAtDesc(
                        HotelContext.current().value(), PageRequest.of(0, limit))
                .stream().map(this::toDomain).toList();
    }

    // ── Mapping ─────────────────────────────────────────────────────────

    private ChannelInboundBookingJpaEntity toJpa(ChannelInboundBooking b) {
        return ChannelInboundBookingJpaEntity.builder()
                .id(b.getId())
                .hotelId(b.getHotelId().value())
                .externalBookingId(b.getExternalBookingId())
                .reservationId(b.getReservationId() == null ? null : b.getReservationId().value())
                .lastRevisionId(b.getLastRevisionId())
                .status(b.getStatus())
                .otaName(b.getOtaName())
                .otaReservationCode(b.getOtaReservationCode())
                .build();
    }

    private ChannelInboundBooking toDomain(ChannelInboundBookingJpaEntity e) {
        return ChannelInboundBooking.reconstitute(
                e.getId(),
                HotelId.of(e.getHotelId()),
                e.getExternalBookingId(),
                e.getReservationId() == null ? null : ReservationId.of(e.getReservationId()),
                e.getLastRevisionId(),
                e.getStatus(),
                e.getOtaName(),
                e.getOtaReservationCode(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
