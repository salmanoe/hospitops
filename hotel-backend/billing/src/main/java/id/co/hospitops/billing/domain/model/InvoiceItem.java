package id.co.hospitops.billing.domain.model;

import id.co.hospitops.shared.Money;

import java.util.UUID;

public record InvoiceItem(
        UUID id,
        String description,
        int quantity,
        Money unitPrice,
        Money totalPrice
) {
}
