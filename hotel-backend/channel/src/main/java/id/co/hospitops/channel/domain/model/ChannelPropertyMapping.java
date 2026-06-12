package id.co.hospitops.channel.domain.model;

import id.co.hospitops.shared.ChannelPropertyMappingId;
import id.co.hospitops.shared.HotelId;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Links one HospitOps hotel to its counterpart property on a channel
 * provider. Enabling the mapping is what turns OTA distribution on for a
 * hotel — until then the channel module stays inert for that tenant.
 */
@Getter
public class ChannelPropertyMapping {

    private final ChannelPropertyMappingId id;
    private final HotelId hotelId;
    private final ChannelProvider provider;
    private String externalPropertyId;
    private boolean enabled;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ChannelPropertyMapping create(HotelId hotelId, ChannelProvider provider,
                                                String externalPropertyId) {
        validateExternalId(externalPropertyId);
        return new ChannelPropertyMapping(
                ChannelPropertyMappingId.generate(), hotelId, provider,
                externalPropertyId, false,
                LocalDateTime.now(), LocalDateTime.now());
    }

    public static ChannelPropertyMapping reconstitute(ChannelPropertyMappingId id, HotelId hotelId,
                                                      ChannelProvider provider, String externalPropertyId,
                                                      boolean enabled, LocalDateTime createdAt,
                                                      LocalDateTime updatedAt) {
        return new ChannelPropertyMapping(id, hotelId, provider, externalPropertyId,
                enabled, createdAt, updatedAt);
    }

    private ChannelPropertyMapping(ChannelPropertyMappingId id, HotelId hotelId, ChannelProvider provider,
                                   String externalPropertyId, boolean enabled,
                                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.hotelId = hotelId;
        this.provider = provider;
        this.externalPropertyId = externalPropertyId;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void updateExternalPropertyId(String externalPropertyId) {
        validateExternalId(externalPropertyId);
        this.externalPropertyId = externalPropertyId;
        this.updatedAt = LocalDateTime.now();
    }

    public void enable() {
        this.enabled = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void disable() {
        this.enabled = false;
        this.updatedAt = LocalDateTime.now();
    }

    private static void validateExternalId(String externalPropertyId) {
        if (externalPropertyId == null || externalPropertyId.isBlank())
            throw new IllegalArgumentException("External property id cannot be blank");
    }
}
