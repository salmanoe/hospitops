package id.co.hospitops.shared;

import java.util.UUID;

public record GroupId(UUID value) implements DomainId {
    public static GroupId generate() {
        return new GroupId(UUID.randomUUID());
    }

    public static GroupId of(UUID value) {
        return new GroupId(value);
    }

    public static GroupId of(String value) {
        return new GroupId(UUID.fromString(value));
    }
}
