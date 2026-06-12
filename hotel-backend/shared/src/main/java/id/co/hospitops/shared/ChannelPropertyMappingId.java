package id.co.hospitops.shared;

import java.util.UUID;

public record ChannelPropertyMappingId(UUID value) implements DomainId {
    public static ChannelPropertyMappingId generate() {
        return new ChannelPropertyMappingId(UUID.randomUUID());
    }

    public static ChannelPropertyMappingId of(UUID value) {
        return new ChannelPropertyMappingId(value);
    }

    public static ChannelPropertyMappingId of(String value) {
        return new ChannelPropertyMappingId(UUID.fromString(value));
    }
}
