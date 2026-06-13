package id.co.hospitops.channel.application.response;

import id.co.hospitops.channel.domain.model.ChannelSyncMessage;
import id.co.hospitops.channel.domain.model.SyncMessageType;
import id.co.hospitops.channel.domain.model.SyncStatus;
import id.co.hospitops.shared.ChannelSyncMessageId;

import java.time.LocalDateTime;

public record ChannelSyncMessageResponse(
        ChannelSyncMessageId id,
        SyncMessageType type,
        SyncStatus status,
        int attempts,
        String lastError,
        LocalDateTime nextAttemptAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ChannelSyncMessageResponse from(ChannelSyncMessage m) {
        return new ChannelSyncMessageResponse(
                m.getId(), m.getType(), m.getStatus(), m.getAttempts(),
                m.getLastError(), m.getNextAttemptAt(), m.getCreatedAt(), m.getUpdatedAt());
    }
}
