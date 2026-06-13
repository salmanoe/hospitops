package id.co.hospitops.channel.application;

import id.co.hospitops.channel.domain.model.*;
import id.co.hospitops.channel.domain.port.out.ChannelInboundBookingRepository;
import id.co.hospitops.channel.domain.port.out.ChannelPropertyMappingRepository;
import id.co.hospitops.channel.domain.port.out.ChannelRoomTypeMappingRepository;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.ReservationId;
import id.co.hospitops.shared.RoomTypeId;
import id.co.hospitops.shared.channel.OtaBookingPort;
import id.co.hospitops.shared.channel.OtaBookingRequest;
import id.co.hospitops.shared.channel.OtaBookingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("ChannelInboundService")
@ExtendWith(MockitoExtension.class)
class ChannelInboundServiceTest {

    @Mock ChannelPropertyMappingRepository propertyRepo;
    @Mock ChannelRoomTypeMappingRepository roomTypeRepo;
    @Mock ChannelInboundBookingRepository inboundRepo;
    @Mock OtaBookingPort otaBookingPort;

    ChannelInboundService service;

    @BeforeEach
    void setUp() {
        service = new ChannelInboundService(propertyRepo, roomTypeRepo, inboundRepo, otaBookingPort);
    }

    private static final String EXT_PROP = "ext-prop";
    private static final String EXT_RT = "ext-rt";

    private ChannelPropertyMapping property() {
        return ChannelPropertyMapping.create(HotelId.generate(), ChannelProvider.CHANNEX, EXT_PROP);
    }

    private BookingRevision revision(RevisionStatus status) {
        return new BookingRevision("rev-1", "bk-1", status, EXT_PROP, "Booking.com", "OTA-123",
                "Jane Doe", "jane@example.com", "+62811", "ID",
                List.of(new BookingRevision.RoomSegment(EXT_RT, "ext-rp",
                        LocalDate.of(2027, 7, 1), LocalDate.of(2027, 7, 3), 2, 0)));
    }

    @Test
    @DisplayName("new booking creates a reservation and records it BOOKED")
    void newBookingCreatesReservation() {
        RoomTypeId rt = RoomTypeId.generate();
        ReservationId resId = ReservationId.generate();
        when(propertyRepo.findByExternalProperty(ChannelProvider.CHANNEX, EXT_PROP)).thenReturn(Optional.of(property()));
        when(inboundRepo.findByExternalBookingId("bk-1")).thenReturn(Optional.empty());
        when(roomTypeRepo.findByExternalRoomTypeId(EXT_RT)).thenReturn(
                Optional.of(ChannelRoomTypeMapping.create(HotelId.generate(), rt, EXT_RT, "ext-rp")));
        when(otaBookingPort.createBooking(any(OtaBookingRequest.class)))
                .thenReturn(Optional.of(new OtaBookingResult(resId, "RES-1")));
        when(inboundRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.process(revision(RevisionStatus.NEW));

        ArgumentCaptor<ChannelInboundBooking> captor = ArgumentCaptor.forClass(ChannelInboundBooking.class);
        verify(inboundRepo).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(InboundStatus.BOOKED);
        assertThat(captor.getValue().getReservationId()).isEqualTo(resId);
    }

    @Test
    @DisplayName("overbooking (no room free) is recorded as CONFLICT, not crashed")
    void overbookingRecordsConflict() {
        when(propertyRepo.findByExternalProperty(ChannelProvider.CHANNEX, EXT_PROP)).thenReturn(Optional.of(property()));
        when(inboundRepo.findByExternalBookingId("bk-1")).thenReturn(Optional.empty());
        when(roomTypeRepo.findByExternalRoomTypeId(EXT_RT)).thenReturn(
                Optional.of(ChannelRoomTypeMapping.create(HotelId.generate(), RoomTypeId.generate(), EXT_RT, "ext-rp")));
        when(otaBookingPort.createBooking(any(OtaBookingRequest.class))).thenReturn(Optional.empty());
        when(inboundRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.process(revision(RevisionStatus.NEW));

        ArgumentCaptor<ChannelInboundBooking> captor = ArgumentCaptor.forClass(ChannelInboundBooking.class);
        verify(inboundRepo).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(InboundStatus.CONFLICT);
        assertThat(captor.getValue().getReservationId()).isNull();
    }

    @Test
    @DisplayName("cancellation cancels the linked reservation and records CANCELLED")
    void cancellationCancelsReservation() {
        ReservationId resId = ReservationId.generate();
        ChannelInboundBooking existing = ChannelInboundBooking.create(HotelId.generate(), "bk-1", "Booking.com");
        existing.markBooked(resId, "rev-0", "OTA-123");
        when(propertyRepo.findByExternalProperty(ChannelProvider.CHANNEX, EXT_PROP)).thenReturn(Optional.of(property()));
        when(inboundRepo.findByExternalBookingId("bk-1")).thenReturn(Optional.of(existing));
        when(inboundRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.process(revision(RevisionStatus.CANCELLED));

        verify(otaBookingPort).cancelBooking(resId);
        ArgumentCaptor<ChannelInboundBooking> captor = ArgumentCaptor.forClass(ChannelInboundBooking.class);
        verify(inboundRepo).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(InboundStatus.CANCELLED);
    }

    @Test
    @DisplayName("revision for an unknown property is skipped (no booking work)")
    void unknownPropertySkipped() {
        when(propertyRepo.findByExternalProperty(ChannelProvider.CHANNEX, EXT_PROP)).thenReturn(Optional.empty());

        service.process(revision(RevisionStatus.NEW));

        verifyNoInteractions(otaBookingPort, inboundRepo, roomTypeRepo);
    }
}
