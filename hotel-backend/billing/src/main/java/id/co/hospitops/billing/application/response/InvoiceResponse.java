package id.co.hospitops.billing.application.response;

import id.co.hospitops.billing.domain.model.*;
import id.co.hospitops.shared.InvoiceId;
import id.co.hospitops.shared.ReservationId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record InvoiceResponse(
        InvoiceId id,
        String invoiceNumber,
        ReservationId reservationId,
        String reservationNumber,
        String guestName,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        BigDecimal totalPaid,
        BigDecimal balance,
        PaymentStatus paymentStatus,
        LocalDate dueDate,
        String notes,
        List<InvoiceItemResponse> items,
        List<PaymentResponse> payments,
        LocalDateTime issuedAt
) {
    public static InvoiceResponse from(Invoice inv) {
        return new InvoiceResponse(
                inv.getId(), inv.getInvoiceNumber(), inv.getReservationId(),
                inv.getReservationNumber(), inv.getGuestName(),
                inv.getSubtotal().amount(), inv.getTaxAmount().amount(),
                inv.getDiscountAmount().amount(), inv.getTotalAmount().amount(),
                inv.getTotalPaid().amount(), inv.getBalance().amount(),
                inv.getPaymentStatus(), inv.getDueDate(), inv.getNotes(),
                inv.getItems().stream().map(InvoiceItemResponse::from).toList(),
                inv.getPayments().stream().map(PaymentResponse::from).toList(),
                inv.getIssuedAt());
    }
}
