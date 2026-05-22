package id.co.hospitops.billing.infrastructure.persistence;

import id.co.hospitops.billing.domain.model.*;
import id.co.hospitops.billing.domain.port.out.InvoiceRepository;
import id.co.hospitops.shared.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@RequiredArgsConstructor
public class InvoiceRepositoryImpl implements InvoiceRepository {

    private final InvoiceJpaRepository     jpa;
    private final InvoiceItemJpaRepository itemJpa;
    private final PaymentJpaRepository     paymentJpa;

    @Override
    public Invoice save(Invoice invoice) {
        jpa.save(toJpa(invoice));
        itemJpa.saveAll(itemsToJpa(invoice));
        paymentJpa.saveAll(paymentsToJpa(invoice));
        // Return the domain object directly — it already carries the full state.
        return invoice;
    }

    @Override
    public Optional<Invoice> findById(InvoiceId id) {
        return jpa.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<Invoice> findAll(Pageable pageable) {
        return jpa.findAll(pageable).map(this::toDomain).getContent();
    }

    @Override
    public List<Invoice> findByStatus(PaymentStatus status, Pageable pageable) {
        return jpa.findByPaymentStatus(status, pageable)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public long count() {
        return jpa.count();
    }

    // ── Mappers ────────────────────────────────────────────────────

    private InvoiceJpaEntity toJpa(Invoice inv) {
        return InvoiceJpaEntity.builder()
                .id(inv.getId().value())
                .invoiceNumber(inv.getInvoiceNumber())
                .reservationId(inv.getReservationId().value())
                .reservationNumber(inv.getReservationNumber())
                .guestName(inv.getGuestName())
                .subtotal(inv.getSubtotal().amount())
                .taxAmount(inv.getTaxAmount().amount())
                .discountAmount(inv.getDiscountAmount().amount())
                .totalAmount(inv.getTotalAmount().amount())
                .paymentStatus(inv.getPaymentStatus())
                .dueDate(inv.getDueDate())
                .notes(inv.getNotes())
                .build();
    }

    private List<InvoiceItemJpaEntity> itemsToJpa(Invoice inv) {
        return inv.getItems().stream()
                .map(i -> InvoiceItemJpaEntity.builder()
                        .id(i.id())
                        .invoiceId(inv.getId().value())
                        .description(i.description())
                        .quantity(i.quantity())
                        .unitPrice(i.unitPrice().amount())
                        .totalPrice(i.totalPrice().amount())
                        .build())
                .toList();
    }

    private List<PaymentJpaEntity> paymentsToJpa(Invoice inv) {
        return inv.getPayments().stream()
                .map(p -> PaymentJpaEntity.builder()
                        .id(p.id())
                        .invoiceId(inv.getId().value())
                        .amount(p.amount().amount())
                        .method(p.method())
                        .referenceNo(p.referenceNo())
                        .paidAt(p.paidAt())
                        .receivedBy(p.receivedBy() != null ? p.receivedBy().value() : null)
                        .build())
                .toList();
    }

    private Invoice toDomain(InvoiceJpaEntity e) {
        List<InvoiceItem> items = itemJpa.findByInvoiceId(e.getId()).stream()
                .map(i -> new InvoiceItem(
                        i.getId(), i.getDescription(), i.getQuantity(),
                        Money.of(i.getUnitPrice()), Money.of(i.getTotalPrice())))
                .toList();

        List<Payment> payments = paymentJpa.findByInvoiceId(e.getId()).stream()
                .map(p -> new Payment(
                        p.getId(),
                        InvoiceId.of(e.getId()),
                        Money.of(p.getAmount()),
                        p.getMethod(),
                        p.getReferenceNo(),
                        p.getReceivedBy() != null ? StaffId.of(p.getReceivedBy()) : null,
                        p.getPaidAt()))
                .toList();

        return Invoice.reconstitute(
                InvoiceId.of(e.getId()),
                e.getInvoiceNumber(),
                ReservationId.of(e.getReservationId()),
                e.getReservationNumber(),
                e.getGuestName(),
                new ArrayList<>(items),
                new ArrayList<>(payments),
                Money.of(e.getSubtotal()),
                Money.of(e.getTaxAmount()),
                Money.of(e.getDiscountAmount()),
                Money.of(e.getTotalAmount()),
                e.getPaymentStatus(),
                e.getDueDate(),
                e.getNotes(),
                e.getIssuedAt(),
                e.getUpdatedAt()
        );
    }
}
