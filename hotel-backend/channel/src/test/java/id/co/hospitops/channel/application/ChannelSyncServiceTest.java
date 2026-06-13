package id.co.hospitops.channel.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import id.co.hospitops.channel.application.payload.ChannelSyncPayload;
import id.co.hospitops.channel.domain.model.ChannelProvider;
import id.co.hospitops.channel.domain.model.ChannelPropertyMapping;
import id.co.hospitops.channel.domain.model.ChannelRoomTypeMapping;
import id.co.hospitops.channel.domain.model.ChannelSyncMessage;
import id.co.hospitops.channel.domain.port.out.ChannelPropertyMappingRepository;
import id.co.hospitops.channel.domain.port.out.ChannelRoomTypeMappingRepository;
import id.co.hospitops.channel.domain.port.out.ChannelSyncMessageRepository;
import id.co.hospitops.shared.HotelContext;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.Money;
import id.co.hospitops.shared.RoomId;
import id.co.hospitops.shared.RoomTypeId;
import id.co.hospitops.shared.channel.RoomAvailabilitySnapshotProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("ChannelSyncService.syncRoomNights")
@ExtendWith(MockitoExtension.class)
class ChannelSyncServiceTest {

    @Mock ChannelPropertyMappingRepository propertyRepo;
    @Mock ChannelRoomTypeMappingRepository roomTypeRepo;
    @Mock ChannelSyncMessageRepository syncRepo;
    @Mock RoomAvailabilitySnapshotProvider snapshot;

    final ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();

    ChannelSyncService service;

    @BeforeEach
    void setUp() {
        service = new ChannelSyncService(propertyRepo, roomTypeRepo, syncRepo, snapshot, objectMapper);
    }

    private static ChannelPropertyMapping enabledProperty() {
        ChannelPropertyMapping p = ChannelPropertyMapping.create(
                HotelId.generate(), ChannelProvider.CHANNEX, "prop-1");
        p.enable();
        return p;
    }

    private final RoomId roomId = RoomId.generate();
    private final LocalDate from = LocalDate.of(2026, 6, 20);
    private final LocalDate to = LocalDate.of(2026, 6, 22); // 2 nights

    @Test
    @DisplayName("no-op when no channel property is configured")
    void noopWhenNotConfigured() {
        when(propertyRepo.findByProvider(ChannelProvider.CHANNEX)).thenReturn(Optional.empty());
        service.syncRoomNights(roomId, from, to);
        verify(syncRepo, never()).save(any());
    }

    @Test
    @DisplayName("no-op when the channel is disabled")
    void noopWhenDisabled() {
        ChannelPropertyMapping disabled = ChannelPropertyMapping.create(
                HotelId.generate(), ChannelProvider.CHANNEX, "prop-1"); // not enabled
        when(propertyRepo.findByProvider(ChannelProvider.CHANNEX)).thenReturn(Optional.of(disabled));
        service.syncRoomNights(roomId, from, to);
        verify(syncRepo, never()).save(any());
    }

    @Test
    @DisplayName("no-op when the room type is not mapped")
    void noopWhenRoomTypeUnmapped() {
        RoomTypeId rt = RoomTypeId.generate();
        when(propertyRepo.findByProvider(ChannelProvider.CHANNEX)).thenReturn(Optional.of(enabledProperty()));
        when(snapshot.roomTypeOf(roomId)).thenReturn(Optional.of(rt));
        when(roomTypeRepo.findByRoomTypeId(rt)).thenReturn(Optional.empty());
        service.syncRoomNights(roomId, from, to);
        verify(syncRepo, never()).save(any());
    }

    @Test
    @DisplayName("enqueues one ARI message covering each night when configured and mapped")
    void enqueuesPerNight() throws Exception {
        RoomTypeId rt = RoomTypeId.generate();
        ChannelRoomTypeMapping mapping = ChannelRoomTypeMapping.create(
                HotelId.generate(), rt, "chx-rt", "chx-rp");
        when(propertyRepo.findByProvider(ChannelProvider.CHANNEX)).thenReturn(Optional.of(enabledProperty()));
        when(snapshot.roomTypeOf(roomId)).thenReturn(Optional.of(rt));
        when(roomTypeRepo.findByRoomTypeId(rt)).thenReturn(Optional.of(mapping));
        when(snapshot.availableUnits(eq(rt), any())).thenReturn(3);
        when(snapshot.ratePerNight(eq(rt), any())).thenReturn(Money.of(new BigDecimal("500000")));

        ArgumentCaptor<ChannelSyncMessage> captor = ArgumentCaptor.forClass(ChannelSyncMessage.class);

        ScopedValue.where(HotelContext.HOTEL_ID, HotelId.generate())
                .run(() -> service.syncRoomNights(roomId, from, to));

        verify(syncRepo).save(captor.capture());
        ChannelSyncPayload payload = objectMapper.readValue(captor.getValue().getPayload(), ChannelSyncPayload.class);
        assertThat(payload.propertyId()).isEqualTo("prop-1");
        assertThat(payload.updates()).hasSize(2); // two nights
        assertThat(payload.updates().getFirst().availability()).isEqualTo(3);
        assertThat(payload.updates().getFirst().externalRoomTypeId()).isEqualTo("chx-rt");
        // IDR has 2 ISO-4217 fraction digits (verified against the Channex
        // staging round-trip), so 500000 major units → 50000000 minor units.
        assertThat(payload.updates().getFirst().rate()).isEqualTo(50000000L);
    }
}
