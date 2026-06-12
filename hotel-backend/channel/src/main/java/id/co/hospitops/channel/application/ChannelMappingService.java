package id.co.hospitops.channel.application;

import id.co.hospitops.channel.application.command.ConfigureChannelPropertyCommand;
import id.co.hospitops.channel.application.command.MapRoomTypeCommand;
import id.co.hospitops.channel.application.response.ChannelPropertyMappingResponse;
import id.co.hospitops.channel.application.response.ChannelRoomTypeMappingResponse;
import id.co.hospitops.channel.domain.model.ChannelProvider;
import id.co.hospitops.channel.domain.model.ChannelPropertyMapping;
import id.co.hospitops.channel.domain.model.ChannelRoomTypeMapping;
import id.co.hospitops.channel.domain.port.in.ManageChannelMappingUseCase;
import id.co.hospitops.channel.domain.port.out.ChannelPropertyMappingRepository;
import id.co.hospitops.channel.domain.port.out.ChannelRoomTypeMappingRepository;
import id.co.hospitops.shared.HotelContext;
import id.co.hospitops.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Manages channel provider mappings for the current hotel. This is pure
 * configuration — it does not call the provider. The connector (Slice 2)
 * reads these mappings to know where to push ARI and how to resolve
 * inbound OTA bookings.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ChannelMappingService implements ManageChannelMappingUseCase {

    // Phase 2 ships a single provider. When more are added this becomes a
    // command parameter instead of a constant.
    private static final ChannelProvider PROVIDER = ChannelProvider.CHANNEX;

    private final ChannelPropertyMappingRepository propertyRepo;
    private final ChannelRoomTypeMappingRepository roomTypeRepo;

    @Override
    public ChannelPropertyMappingResponse configureProperty(ConfigureChannelPropertyCommand cmd) {
        ChannelPropertyMapping mapping = propertyRepo.findByProvider(PROVIDER)
                .map(existing -> {
                    existing.updateExternalPropertyId(cmd.externalPropertyId());
                    return existing;
                })
                .orElseGet(() -> ChannelPropertyMapping.create(
                        HotelContext.current(), PROVIDER, cmd.externalPropertyId()));
        return ChannelPropertyMappingResponse.from(propertyRepo.save(mapping));
    }

    @Override
    @Transactional(readOnly = true)
    public ChannelPropertyMappingResponse getProperty() {
        return ChannelPropertyMappingResponse.from(requireProperty());
    }

    @Override
    public ChannelPropertyMappingResponse enableChannel() {
        ChannelPropertyMapping m = requireProperty();
        m.enable();
        return ChannelPropertyMappingResponse.from(propertyRepo.save(m));
    }

    @Override
    public ChannelPropertyMappingResponse disableChannel() {
        ChannelPropertyMapping m = requireProperty();
        m.disable();
        return ChannelPropertyMappingResponse.from(propertyRepo.save(m));
    }

    @Override
    public ChannelRoomTypeMappingResponse mapRoomType(MapRoomTypeCommand cmd) {
        ChannelRoomTypeMapping mapping = roomTypeRepo.findByRoomTypeId(cmd.roomTypeId())
                .map(existing -> {
                    existing.update(cmd.externalRoomTypeId(), cmd.externalRatePlanId());
                    return existing;
                })
                .orElseGet(() -> ChannelRoomTypeMapping.create(
                        HotelContext.current(), cmd.roomTypeId(),
                        cmd.externalRoomTypeId(), cmd.externalRatePlanId()));
        return ChannelRoomTypeMappingResponse.from(roomTypeRepo.save(mapping));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChannelRoomTypeMappingResponse> listRoomTypeMappings() {
        return roomTypeRepo.findAll().stream()
                .map(ChannelRoomTypeMappingResponse::from)
                .toList();
    }

    private ChannelPropertyMapping requireProperty() {
        return propertyRepo.findByProvider(PROVIDER)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ChannelPropertyMapping", "no " + PROVIDER + " property configured for this hotel"));
    }
}
