package id.co.hospitops.shared;

import java.util.UUID;

public record ChannelRoomTypeMappingId(UUID value) implements DomainId {
    public static ChannelRoomTypeMappingId generate() {
        return new ChannelRoomTypeMappingId(UUID.randomUUID());
    }

    public static ChannelRoomTypeMappingId of(UUID value) {
        return new ChannelRoomTypeMappingId(value);
    }

    public static ChannelRoomTypeMappingId of(String value) {
        return new ChannelRoomTypeMappingId(UUID.fromString(value));
    }
}
