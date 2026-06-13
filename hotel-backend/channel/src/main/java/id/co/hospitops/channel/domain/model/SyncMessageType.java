package id.co.hospitops.channel.domain.model;

/** What an outbox message asks the provider to do. */
public enum SyncMessageType {
    /** Push availability + rates (ARI) for one or more room-type nights. */
    ARI_PUSH
}
