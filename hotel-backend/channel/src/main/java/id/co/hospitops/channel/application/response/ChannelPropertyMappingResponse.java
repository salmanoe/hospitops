package id.co.hospitops.channel.application.response;

import id.co.hospitops.channel.domain.model.ChannelPropertyMapping;
import id.co.hospitops.channel.domain.model.ChannelProvider;
import id.co.hospitops.shared.ChannelPropertyMappingId;

import java.time.LocalDateTime;

public record ChannelPropertyMappingResponse(
        ChannelPropertyMappingId id,
        ChannelProvider          provider,
        String                   externalPropertyId,
        boolean                  enabled,
        LocalDateTime            createdAt,
        LocalDateTime            updatedAt
) {
    public static ChannelPropertyMappingResponse from(ChannelPropertyMapping m) {
        return new ChannelPropertyMappingResponse(
                m.getId(), m.getProvider(), m.getExternalPropertyId(),
                m.isEnabled(), m.getCreatedAt(), m.getUpdatedAt());
    }
}
