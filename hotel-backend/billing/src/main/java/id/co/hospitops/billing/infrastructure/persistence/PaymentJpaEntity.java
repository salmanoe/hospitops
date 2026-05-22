package id.co.hospitops.billing.infrastructure.persistence;

import id.co.hospitops.billing.domain.model.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment", indexes = {
        @Index(name = "idx_payment_invoice_id", columnList = "invoice_id"),
        @Index(name = "idx_payment_paid_at",    columnList = "paid_at"),
        @Index(name = "idx_payment_method",     columnList = "method")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class PaymentJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "invoice_id", nullable = false, columnDefinition = "uuid")
    private UUID invoiceId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private PaymentMethod method;

    @Column(name = "reference_no")
    private String referenceNo;

    // Stored explicitly from the domain object — do NOT use @CreationTimestamp,
    // which would overwrite paidAt on every merge during subsequent invoice saves.
    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt;

    @Column(name = "received_by", columnDefinition = "uuid")
    private UUID receivedBy;
}
