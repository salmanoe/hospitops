package id.co.hospitops.shared;

import java.util.UUID;

public record PolicyConfigId(UUID value) implements DomainId {
    public static PolicyConfigId generate() {
        return new PolicyConfigId(UUID.randomUUID());
    }

    public static PolicyConfigId of(UUID value) {
        return new PolicyConfigId(value);
    }

    public static PolicyConfigId of(String value) {
        return new PolicyConfigId(UUID.fromString(value));
    }
}
