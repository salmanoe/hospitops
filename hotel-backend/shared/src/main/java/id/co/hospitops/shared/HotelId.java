package id.co.hospitops.shared;

import java.util.UUID;

public record HotelId(UUID value) implements DomainId {
    public static HotelId generate() {
        return new HotelId(UUID.randomUUID());
    }

    public static HotelId of(UUID value) {
        return new HotelId(value);
    }

    public static HotelId of(String value) {
        return new HotelId(UUID.fromString(value));
    }
}
