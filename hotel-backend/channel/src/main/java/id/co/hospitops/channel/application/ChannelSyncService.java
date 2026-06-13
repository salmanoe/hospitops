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
import id.co.hospitops.shared.exception.BusinessRuleViolationException;
import id.co.hospitops.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        List<AriUpdate> updates = command.nights().stream()
                .map(n -> new AriUpdate(rt.getExternalRoomTypeId(), rt.getExternalRatePlanId(),
                        n.date(), n.availability(), n.rate()))
                .toList();

        String payload = serialize(new ChannelSyncPayload(property.getExternalPropertyId(), updates));
        syncRepo.save(ChannelSyncMessage.create(HotelContext.current(), SyncMessageType.ARI_PUSH, payload));
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
