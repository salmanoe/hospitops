package id.co.hospitops.channel.domain.model;

/** Status of an inbound booking revision from the provider. */
public enum RevisionStatus {
    NEW,
    MODIFIED,
    CANCELLED,
    UNKNOWN
}
