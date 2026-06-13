package id.co.hospitops.channel.domain.model;

/**
 * Channel-connectivity providers HospitOps can distribute through.
 *
 * <p>Phase 2 ships {@link #CHANNEX} only — an aggregator that holds the OTA
 * certifications (Booking.com, Agoda, Traveloka, Expedia…) behind one API.
 * Additional providers (or direct OTA integrations) can be added later
 * without changing the mapping model.
 */
public enum ChannelProvider {
    CHANNEX
}
