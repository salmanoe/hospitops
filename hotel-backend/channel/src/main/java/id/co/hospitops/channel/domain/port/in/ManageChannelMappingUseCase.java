package id.co.hospitops.channel.domain.port.in;

import id.co.hospitops.channel.application.command.ConfigureChannelPropertyCommand;
import id.co.hospitops.channel.application.command.MapRoomTypeCommand;
import id.co.hospitops.channel.application.response.ChannelPropertyMappingResponse;
import id.co.hospitops.channel.application.response.ChannelRoomTypeMappingResponse;

import java.util.List;

/**
 * Manages how the current hotel is mapped onto the channel provider —
 * the configuration staff set up before ARI sync can run.
 */
public interface ManageChannelMappingUseCase {

    /** Create or update the hotel's provider property hookup. */
    ChannelPropertyMappingResponse configureProperty(ConfigureChannelPropertyCommand cmd);

    /** The hotel's property hookup, if configured. */
    ChannelPropertyMappingResponse getProperty();

    ChannelPropertyMappingResponse enableChannel();

    ChannelPropertyMappingResponse disableChannel();

    /** Create or update the provider mapping for one room type. */
    ChannelRoomTypeMappingResponse mapRoomType(MapRoomTypeCommand cmd);

    List<ChannelRoomTypeMappingResponse> listRoomTypeMappings();
}
