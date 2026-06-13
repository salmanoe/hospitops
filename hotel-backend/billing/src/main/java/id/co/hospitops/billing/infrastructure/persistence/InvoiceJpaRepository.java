package id.co.hospitops.billing.infrastructure.persistence;

import id.co.hospitops.billing.domain.model.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceJpaRepository extends JpaRepository<InvoiceJpaEntity, UUID> {
    Optional<InvoiceJpaEntity> findByIdAndHotelId(UUID id, UUID hotelId);

    Page<InvoiceJpaEntity> findByHotelId(UUID hotelId, Pageable pageable);

    Page<InvoiceJpaEntity> findByHotelIdAndPaymentStatus(UUID hotelId, PaymentStatus status,
                                                         Pageable pageable);

    long countByHotelId(UUID hotelId);
}
