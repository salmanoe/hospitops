package id.co.hospitops.billing.application;

// R-07 FIX: Removed the spurious PaymentReceivedEvent emitted on invoice creation
// (no payment had occurred). PaymentReceivedEvent is published only by recordPayment().
//
// R3-03 FIX: @EventListener removed (SRP violation).
// Event handling lives in BillingEventHandler; this class is pure business logic.

import id.co.hospitops.billing.application.command.RecordPaymentCommand;
import id.co.hospitops.billing.application.response.InvoiceResponse;
import id.co.hospitops.billing.domain.model.Invoice;
import id.co.hospitops.billing.domain.model.PaymentStatus;
import id.co.hospitops.shared.exception.BusinessRuleViolationException;
import id.co.hospitops.billing.domain.port.in.BillingUseCase;
import id.co.hospitops.billing.domain.port.out.InvoiceNumberGenerator;
import id.co.hospitops.billing.domain.port.out.InvoiceRepository;
import id.co.hospitops.billing.domain.port.out.ReservationDetailPort;
import id.co.hospitops.billing.infrastructure.pdf.InvoicePdfGenerator;
import id.co.hospitops.shared.HotelContext;
import id.co.hospitops.shared.InvoiceId;
import id.co.hospitops.shared.Money;
import id.co.hospitops.shared.ReservationId;
import id.co.hospitops.shared.event.PaymentReceivedEvent;
import id.co.hospitops.shared.exception.ResourceNotFoundException;
import id.co.hospitops.shared.web.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class BillingService implements BillingUseCase {

    private final InvoiceRepository invoiceRepo;
    private final InvoiceNumberGenerator numberGenerator;
    private final ReservationDetailPort reservationDetail;
    private final InvoicePdfGenerator pdfGenerator;
    private final ApplicationEventPublisher eventPublisher;

    // ── BillingUseCase: createInvoiceForCheckout ──────────────────
    // R3-03 FIX: Formerly the @EventListener onCheckout() body lived here,
    // mixing event-handling with business logic (SRP violation).
    // The @EventListener is now in BillingEventHandler, which delegates here.
    @Override
    @Transactional
    public void createInvoiceForCheckout(ReservationId reservationId, long nights) {
        log.info("Generating invoice for reservation {}", reservationId);

        ReservationDetailPort.ReservationDetail detail =
                reservationDetail.findById(reservationId);

        String number = numberGenerator.generate();
        Invoice invoice = Invoice.create(
                HotelContext.current(), number, reservationId,
                detail.reservationNumber(), detail.guestFullName(),
                nights, detail.ratePerNight(), detail.roomTypeName());

        Invoice saved = invoiceRepo.save(invoice);
        log.info("Invoice {} created for reservation {}",
                saved.getInvoiceNumber(), reservationId);

        // R-07 FIX: No PaymentReceivedEvent here. The invoice is UNPAID at this
        // point. PaymentReceivedEvent is published only by recordPayment() once
        // the guest actually settles the bill.
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse findById(InvoiceId id) {
        return InvoiceResponse.from(findInvoice(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InvoiceResponse> findAll(String statusFilter, Pageable pageable) {
        // R3-05 FIX: Mirrors the W-12 fix in ReservationService.findAll().
        // valueOf() throws IllegalArgumentException on unknown values, which
        // previously produced an opaque HTTP 500. Wrap it so callers receive
        // a clean HTTP 400 (BusinessRuleViolationException -> GlobalExceptionHandler).
        if (statusFilter != null && !statusFilter.isBlank()) {
            PaymentStatus status;
            try {
                status = PaymentStatus.valueOf(statusFilter.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessRuleViolationException(
                        "Unknown payment status: " + statusFilter);
            }
            List<InvoiceResponse> filtered = invoiceRepo.findByStatus(status, pageable)
                    .stream().map(InvoiceResponse::from).toList();
            return PageResult.of(filtered, pageable.getPageNumber(),
                    pageable.getPageSize(), invoiceRepo.count());
        }
        List<InvoiceResponse> all = invoiceRepo.findAll(pageable)
                .stream().map(InvoiceResponse::from).toList();
        return PageResult.of(all, pageable.getPageNumber(),
                pageable.getPageSize(), invoiceRepo.count());
    }

    @Override
    public InvoiceResponse recordPayment(InvoiceId id, RecordPaymentCommand cmd) {
        Invoice invoice = findInvoice(id);
        // Invoice.recordPayment() enforces: already-paid guard + overpayment guard (R-12)
        invoice.recordPayment(
                Money.of(cmd.amount()), cmd.method(), cmd.referenceNo(), cmd.receivedBy());
        Invoice saved = invoiceRepo.save(invoice);

        boolean fullyPaid = saved.getPaymentStatus() == PaymentStatus.PAID;
        eventPublisher.publishEvent(new PaymentReceivedEvent(
                saved.getId(), saved.getReservationId(),
                Money.of(cmd.amount()), fullyPaid));

        log.info("Payment of {} recorded for invoice {}", cmd.amount(),
                saved.getInvoiceNumber());
        return InvoiceResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generatePdf(InvoiceId id) {
        Invoice invoice = findInvoice(id);
        ReservationDetailPort.ReservationDetail detail =
                reservationDetail.findById(invoice.getReservationId());
        return pdfGenerator.generate(invoice, detail);
    }

    private Invoice findInvoice(InvoiceId id) {
        return invoiceRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id.value()));
    }
}
