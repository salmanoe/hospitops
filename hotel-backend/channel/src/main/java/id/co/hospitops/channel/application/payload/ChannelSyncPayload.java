package id.co.hospitops.channel.application.payload;

import id.co.hospitops.channel.domain.model.AriUpdate;

import java.util.List;

/**
 * Serialized body of an {@code ARI_PUSH} outbox message: everything the
 * connector needs to deliver, decoupled from any live entity state.
 */
public record ChannelSyncPayload(String propertyId, List<AriUpdate> updates) {
}
