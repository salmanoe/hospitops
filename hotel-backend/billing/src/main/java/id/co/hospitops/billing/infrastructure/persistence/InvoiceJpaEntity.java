package id.co.hospitops.billing.infrastructure.persistence;

import id.co.hospitops.billing.domain.model.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.*;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@ConcreteProxy
@Table(name = "invoice", indexes = {
        @Index(name = "idx_invoice_number", columnList = "invoice_number"),
        @Index(name = "idx_invoice_reservation", columnList = "reservation_id"),
        @Index(name = "idx_invoice_status", columnList = "payment_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, unique = true, length = 20)
    private String invoiceNumber;

    @Column(nullable = false, columnDefinition = "uuid")
    private UUID reservationId;

    @Column(nullable = false, length = 20)
    private String reservationNumber;

    @Column
    private String guestName;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal taxAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus;

    private LocalDate dueDate;

    @Column(columnDefinition = "text")
    private String notes;

    @CreationTimestamp
    private LocalDateTime issuedAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
