package id.co.hospitops.billing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, UUID> {
    List<PaymentJpaEntity> findByInvoiceId(UUID invoiceId);
}
