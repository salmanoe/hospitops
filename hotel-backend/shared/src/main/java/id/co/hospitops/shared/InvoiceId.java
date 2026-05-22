package id.co.hospitops.shared;

import java.util.UUID;

public record InvoiceId(UUID value) implements DomainId {
    public static InvoiceId generate() {
        return new InvoiceId(UUID.randomUUID());
    }

    public static InvoiceId of(UUID value) {
        return new InvoiceId(value);
    }

    public static InvoiceId of(String value) {
        return new InvoiceId(UUID.fromString(value));
    }
}
