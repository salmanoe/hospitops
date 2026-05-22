package id.co.hospitops.shared;

import java.util.UUID;

public record StaffId(UUID value) implements DomainId {
    public static StaffId generate() {
        return new StaffId(UUID.randomUUID());
    }

    public static StaffId of(UUID value) {
        return new StaffId(value);
    }

    public static StaffId of(String value) {
        return new StaffId(UUID.fromString(value));
    }
}
