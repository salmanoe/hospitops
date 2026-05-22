package id.co.hospitops.shared;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.UUID;

public interface DomainId {
    @JsonValue
    UUID value();

    static UUID generate() {
        return UUID.randomUUID();
    }
}
