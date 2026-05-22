package id.co.hospitops.shared;

import java.util.UUID;

public record ReservationId(UUID value) implements DomainId {
    public static ReservationId generate() {
        return new ReservationId(UUID.randomUUID());
    }

    public static ReservationId of(UUID value) {
        return new ReservationId(value);
    }

    public static ReservationId of(String value) {
        return new ReservationId(UUID.fromString(value));
    }
}
