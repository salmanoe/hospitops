package id.co.hospitops.billing.infrastructure.persistence;

import id.co.hospitops.billing.domain.model.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InvoiceJpaRepository extends JpaRepository<InvoiceJpaEntity, UUID> {
    Page<InvoiceJpaEntity> findByPaymentStatus(PaymentStatus status, Pageable pageable);
}
