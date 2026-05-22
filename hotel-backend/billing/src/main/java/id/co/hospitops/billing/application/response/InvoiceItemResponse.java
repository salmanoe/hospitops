package id.co.hospitops.billing.application.response;

import id.co.hospitops.billing.domain.model.InvoiceItem;

import java.math.BigDecimal;

public record InvoiceItemResponse(
        String description,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice
) {
    public static InvoiceItemResponse from(InvoiceItem i) {
        return new InvoiceItemResponse(i.description(), i.quantity(),
                i.unitPrice().amount(), i.totalPrice().amount());
    }
}
