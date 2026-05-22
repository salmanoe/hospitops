package id.co.hospitops.shared;

import java.util.UUID;

public record RoomTypeId(UUID value) implements DomainId {
    public static RoomTypeId generate() {
        return new RoomTypeId(UUID.randomUUID());
    }

    public static RoomTypeId of(UUID value) {
        return new RoomTypeId(value);
    }

    public static RoomTypeId of(String value) {
        return new RoomTypeId(UUID.fromString(value));
    }
}
