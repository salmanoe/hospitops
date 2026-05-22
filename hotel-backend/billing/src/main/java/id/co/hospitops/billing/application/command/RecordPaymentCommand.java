package id.co.hospitops.billing.application.command;

import id.co.hospitops.billing.domain.model.PaymentMethod;
import id.co.hospitops.shared.StaffId;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record RecordPaymentCommand(
        @NotNull @Positive BigDecimal amount,
        @NotNull PaymentMethod method,
        String referenceNo,
        @NotNull StaffId receivedBy
) {
}
