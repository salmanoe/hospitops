package id.co.hospitops.billing.domain.port.out;

import id.co.hospitops.billing.domain.model.Invoice;
import id.co.hospitops.billing.domain.model.PaymentStatus;
import id.co.hospitops.shared.InvoiceId;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository {
    Invoice save(Invoice invoice);

    Optional<Invoice> findById(InvoiceId id);

    List<Invoice> findAll(Pageable pageable);

    List<Invoice> findByStatus(PaymentStatus status, Pageable pageable);

    long count();
}
