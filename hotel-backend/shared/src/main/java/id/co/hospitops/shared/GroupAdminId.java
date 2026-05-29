package id.co.hospitops.shared;

import java.util.UUID;

public record GroupAdminId(UUID value) implements DomainId {
    public static GroupAdminId generate() {
        return new GroupAdminId(UUID.randomUUID());
    }

    public static GroupAdminId of(UUID value) {
        return new GroupAdminId(value);
    }

    public static GroupAdminId of(String value) {
        return new GroupAdminId(UUID.fromString(value));
    }
}
