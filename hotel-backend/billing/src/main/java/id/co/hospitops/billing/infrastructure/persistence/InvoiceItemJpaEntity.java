package id.co.hospitops.billing.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "invoice_item", indexes = {
        @Index(name = "idx_invoice_item_invoice_id", columnList = "invoice_id")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class InvoiceItemJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "invoice_id", nullable = false, columnDefinition = "uuid")
    private UUID invoiceId;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPrice;
}
