package id.co.hospitops.billing.adapter.web;

import id.co.hospitops.billing.domain.model.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record RecordPaymentRequest(
        @NotNull @Positive BigDecimal amount,
        @NotNull PaymentMethod method,
        String referenceNo) {
}
