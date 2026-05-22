package id.co.hospitops.billing.domain.model;

import id.co.hospitops.shared.InvoiceId;
import id.co.hospitops.shared.Money;
import id.co.hospitops.shared.StaffId;

import java.time.LocalDateTime;
import java.util.UUID;

public record Payment(
        UUID id,
        InvoiceId invoiceId,
        Money amount,
        PaymentMethod method,
        String referenceNo,
        StaffId receivedBy,
        LocalDateTime paidAt
) {
}
