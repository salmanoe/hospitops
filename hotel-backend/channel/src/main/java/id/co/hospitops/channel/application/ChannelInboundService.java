package id.co.hospitops.channel.application;

import id.co.hospitops.channel.domain.model.*;
import id.co.hospitops.channel.domain.port.out.ChannelInboundBookingRepository;
import id.co.hospitops.channel.domain.port.out.ChannelPropertyMappingRepository;
import id.co.hospitops.channel.domain.port.out.ChannelRoomTypeMappingRepository;
import id.co.hospitops.shared.HotelContext;
import id.co.hospitops.shared.channel.OtaBookingPort;
import id.co.hospitops.shared.channel.OtaBookingRequest;
import id.co.hospitops.shared.channel.OtaBookingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Applies one inbound booking revision to HospitOps: resolves the owning hotel
 * from the OTA property, then creates / modifies / cancels a reservation,
 * idempotently by the provider's stable booking id, with an overbooking guard.
 *
 * <p>Each revision is processed in its own transaction; throwing leaves it
 * un-acked so the poller retries it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelInboundService {

    private static final ChannelProvider PROVIDER = ChannelProvider.CHANNEX;

    private final ChannelPropertyMappingRepository propertyRepo;
    private final ChannelRoomTypeMappingRepository roomTypeRepo;
    private final ChannelInboundBookingRepository inboundRepo;
    private final OtaBookingPort otaBookingPort;

    @Transactional
    public void process(BookingRevision rev) {
        if (rev.externalPropertyId() == null || rev.bookingId() == null) {
            log.warn("Skipping inbound revision {} — missing property/booking id", rev.revisionId());
            return;
        }
        ChannelPropertyMapping property =
                propertyRepo.findByExternalProperty(PROVIDER, rev.externalPropertyId()).orElse(null);
        if (property == null) {
            log.debug("Inbound revision {} property {} maps to no hotel — skipping",
                    rev.revisionId(), rev.externalPropertyId());
            return;
        }
        ScopedValue.where(HotelContext.HOTEL_ID, property.getHotelId()).run(() -> apply(rev));
    }

    private void apply(BookingRevision rev) {
        ChannelInboundBooking record = inboundRepo.findByExternalBookingId(rev.bookingId())
                .orElseGet(() -> ChannelInboundBooking.create(
                        HotelContext.current(), rev.bookingId(), rev.otaName()));

        switch (rev.status()) {
            case NEW, MODIFIED -> applyBooking(rev, record);
            case CANCELLED -> applyCancellation(rev, record);
            default -> {
                log.warn("Inbound revision {} has unknown status — recording conflict", rev.revisionId());
                record.markConflict(rev.revisionId());
                inboundRepo.save(record);
            }
        }
    }

    private void applyBooking(BookingRevision rev, ChannelInboundBooking record) {
        // Duplicate redelivery of a revision we already booked.
        if (rev.status() == RevisionStatus.NEW
                && record.getStatus() == InboundStatus.BOOKED
                && rev.revisionId() != null
                && rev.revisionId().equals(record.getLastRevisionId())) {
            return;
        }
        // A modification supersedes the previous reservation.
        if (record.getReservationId() != null && record.getStatus() == InboundStatus.BOOKED) {
            otaBookingPort.cancelBooking(record.getReservationId());
        }

        if (rev.rooms() == null || rev.rooms().isEmpty()) {
            record.markConflict(rev.revisionId());
            inboundRepo.save(record);
            return;
        }
        BookingRevision.RoomSegment seg = rev.rooms().getFirst();
        ChannelRoomTypeMapping rt = roomTypeRepo.findByExternalRoomTypeId(seg.externalRoomTypeId()).orElse(null);
        if (rt == null || seg.checkIn() == null || seg.checkOut() == null) {
            log.warn("Inbound revision {}: room type {} unmapped or dates missing — conflict",
                    rev.revisionId(), seg.externalRoomTypeId());
            record.markConflict(rev.revisionId());
            inboundRepo.save(record);
            return;
        }

        Optional<OtaBookingResult> result = otaBookingPort.createBooking(new OtaBookingRequest(
                rt.getRoomTypeId(), seg.checkIn(), seg.checkOut(), seg.adults(), seg.children(),
                rev.guestFullName(), rev.guestEmail(), rev.guestPhone(), rev.guestNationality(),
                otaReference(rev)));

        if (result.isEmpty()) {
            log.warn("Overbooking: no room of type {} free {}..{} for OTA booking {}",
                    rt.getRoomTypeId().value(), seg.checkIn(), seg.checkOut(), rev.bookingId());
            record.markConflict(rev.revisionId());
        } else {
            record.markBooked(result.get().reservationId(), rev.revisionId(), rev.otaReservationCode());
        }
        inboundRepo.save(record);
    }

    private void applyCancellation(BookingRevision rev, ChannelInboundBooking record) {
        if (record.getReservationId() != null && record.getStatus() == InboundStatus.BOOKED) {
            otaBookingPort.cancelBooking(record.getReservationId());
        }
        record.markCancelled(rev.revisionId());
        inboundRepo.save(record);
    }

    private static String otaReference(BookingRevision rev) {
        return ("OTA " + nullToEmpty(rev.otaName()) + " " + nullToEmpty(rev.otaReservationCode())).trim();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
