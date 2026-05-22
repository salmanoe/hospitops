package id.co.hospitops.shared;

import java.util.UUID;

public record RoomId(UUID value) implements DomainId {
    public static RoomId generate() {
        return new RoomId(UUID.randomUUID());
    }

    public static RoomId of(UUID value) {
        return new RoomId(value);
    }

    public static RoomId of(String value) {
        return new RoomId(UUID.fromString(value));
    }
}
