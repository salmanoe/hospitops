package id.co.hospitops.billing.application.response;

import id.co.hospitops.billing.domain.model.Payment;
import id.co.hospitops.billing.domain.model.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        BigDecimal amount,
        PaymentMethod method,
        String referenceNo,
        LocalDateTime paidAt
) {
    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.amount().amount(), p.method(),
                p.referenceNo(), p.paidAt());
    }
}
