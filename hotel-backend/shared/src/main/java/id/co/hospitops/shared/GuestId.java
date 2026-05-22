package id.co.hospitops.shared;

import java.util.UUID;

public record GuestId(UUID value) implements DomainId {
    public static GuestId generate() {
        return new GuestId(UUID.randomUUID());
    }

    public static GuestId of(UUID value) {
        return new GuestId(value);
    }

    public static GuestId of(String value) {
        return new GuestId(UUID.fromString(value));
    }
}
