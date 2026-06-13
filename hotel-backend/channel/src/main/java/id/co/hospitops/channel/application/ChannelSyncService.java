package id.co.hospitops.channel.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.co.hospitops.channel.application.command.PushAriCommand;
import id.co.hospitops.channel.application.payload.ChannelSyncPayload;
import id.co.hospitops.channel.domain.model.AriUpdate;
import id.co.hospitops.channel.domain.model.ChannelProvider;
import id.co.hospitops.channel.domain.model.ChannelPropertyMapping;
import id.co.hospitops.channel.domain.model.ChannelRoomTypeMapping;
import id.co.hospitops.channel.domain.model.ChannelSyncMessage;
import id.co.hospitops.channel.domain.model.SyncMessageType;
import id.co.hospitops.channel.domain.port.in.SyncChannelUseCase;
import id.co.hospitops.channel.domain.port.out.ChannelPropertyMappingRepository;
import id.co.hospitops.channel.domain.port.out.ChannelRoomTypeMappingRepository;
import id.co.hospitops.channel.domain.port.out.ChannelSyncMessageRepository;
import id.co.hospitops.shared.HotelContext;
import id.co.hospitops.shared.Money;
import id.co.hospitops.shared.RoomId;
import id.co.hospitops.shared.RoomTypeId;
import id.co.hospitops.shared.channel.RoomAvailabilitySnapshotProvider;
import id.co.hospitops.shared.exception.BusinessRuleViolationException;
import id.co.hospitops.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

/**
 * Enqueues channel-sync work into the outbox for the current hotel. Delivery is
 * handled asynchronously by {@link ChannelOutboxProcessor}.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ChannelSyncService implements SyncChannelUseCase {

    private static final ChannelProvider PROVIDER = ChannelProvider.CHANNEX;

    private final ChannelPropertyMappingRepository propertyRepo;
    private final ChannelRoomTypeMappingRepository roomTypeRepo;
    private final ChannelSyncMessageRepository syncRepo;
    private final RoomAvailabilitySnapshotProvider snapshot;
    private final ObjectMapper objectMapper;

    @Override
    public void enqueueAriPush(PushAriCommand command) {
        ChannelPropertyMapping property = propertyRepo.findByProvider(PROVIDER)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ChannelPropertyMapping", "channel not configured for this hotel"));
        if (!property.isEnabled()) {
            throw new BusinessRuleViolationException("Channel sync is disabled for this hotel");
        }
        ChannelRoomTypeMapping rt = roomTypeRepo.findByRoomTypeId(command.roomTypeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ChannelRoomTypeMapping", command.roomTypeId().value()));

        // The request rate is in major units; convert to Channex minor units
        // using the room type's currency.
        Currency currency = command.nights().isEmpty()
                ? Money.IDR
                : snapshot.ratePerNight(command.roomTypeId(), command.nights().getFirst().date()).currency();

        List<AriUpdate> updates = command.nights().stream()
                .map(n -> new AriUpdate(rt.getExternalRoomTypeId(), rt.getExternalRatePlanId(),
                        n.date(), n.availability(), MinorUnits.of(n.rate(), currency)))
                .toList();

        String payload = serialize(new ChannelSyncPayload(property.getExternalPropertyId(), updates));
        syncRepo.save(ChannelSyncMessage.create(HotelContext.current(), SyncMessageType.ARI_PUSH, payload));
    }

    /**
     * Event-driven, best-effort: enqueue an availability/rate push for the room
     * type of {@code roomId} across the nights {@code [from, to)}. No-op (never
     * throws) when the hotel has no enabled channel or the room type is unmapped,
     * so it is safe to call from the reservation flow for non-channel hotels.
     */
    public void syncRoomNights(RoomId roomId, LocalDate from, LocalDate to) {
        ChannelPropertyMapping property = propertyRepo.findByProvider(PROVIDER).orElse(null);
        if (property == null || !property.isEnabled()) return;

        RoomTypeId roomTypeId = snapshot.roomTypeOf(roomId).orElse(null);
        if (roomTypeId == null) return;

        ChannelRoomTypeMapping mapping = roomTypeRepo.findByRoomTypeId(roomTypeId).orElse(null);
        if (mapping == null) return;

        List<AriUpdate> updates = new ArrayList<>();
        for (LocalDate night = from; night.isBefore(to); night = night.plusDays(1)) {
            updates.add(new AriUpdate(
                    mapping.getExternalRoomTypeId(), mapping.getExternalRatePlanId(), night,
                    snapshot.availableUnits(roomTypeId, night),
                    MinorUnits.of(snapshot.ratePerNight(roomTypeId, night))));
        }
        if (updates.isEmpty()) return;

        syncRepo.save(ChannelSyncMessage.create(HotelContext.current(), SyncMessageType.ARI_PUSH,
                serialize(new ChannelSyncPayload(property.getExternalPropertyId(), updates))));
    }

    private String serialize(ChannelSyncPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            // Serialising our own records should never fail; treat as a bug.
            throw new IllegalStateException("Failed to serialise channel sync payload", e);
        }
    }
}
