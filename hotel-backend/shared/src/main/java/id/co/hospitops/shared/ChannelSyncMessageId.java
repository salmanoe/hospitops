package id.co.hospitops.shared;

import java.util.UUID;

public record ChannelSyncMessageId(UUID value) implements DomainId {
    public static ChannelSyncMessageId generate() {
        return new ChannelSyncMessageId(UUID.randomUUID());
    }

    public static ChannelSyncMessageId of(UUID value) {
        return new ChannelSyncMessageId(value);
    }
}
