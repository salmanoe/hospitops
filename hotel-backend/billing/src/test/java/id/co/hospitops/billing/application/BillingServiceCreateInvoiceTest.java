package id.co.hospitops.billing.application;

import id.co.hospitops.billing.application.command.RecordPaymentCommand;
import id.co.hospitops.billing.application.response.InvoiceResponse;
import id.co.hospitops.billing.domain.model.Invoice;
import id.co.hospitops.billing.domain.model.PaymentMethod;
import id.co.hospitops.billing.domain.model.PaymentStatus;
import id.co.hospitops.billing.domain.port.in.BillingUseCase;
import id.co.hospitops.billing.domain.port.out.InvoiceNumberGenerator;
import id.co.hospitops.billing.domain.port.out.InvoiceRepository;
import id.co.hospitops.billing.domain.port.out.ReservationDetailPort;
import id.co.hospitops.billing.domain.port.out.ReservationDetailPort.ReservationDetail;
import id.co.hospitops.billing.infrastructure.pdf.InvoicePdfGenerator;
import id.co.hospitops.shared.*;
import id.co.hospitops.shared.event.PaymentReceivedEvent;
import id.co.hospitops.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for BillingService.createInvoiceForCheckout() and recordPayment().
 *
 * Covers:
 *   createInvoiceForCheckout():
 *     - fetches reservation detail and saves the resulting invoice
 *     - uses the number generator to produce a unique invoice number
 *     - does NOT publish PaymentReceivedEvent at creation (R-07 fix)
 *
 *   recordPayment():
 *     - throws ResourceNotFoundException for an unknown invoice ID
 *     - delegates payment to Invoice.recordPayment(), saves, and returns response
 *     - publishes PaymentReceivedEvent after a payment is recorded
 *     - publishes fullyPaid = true when the invoice reaches PAID status
 *     - publishes fullyPaid = false when the invoice is still PARTIAL
 */
@DisplayName("BillingService — createInvoiceForCheckout + recordPayment")
@ExtendWith(MockitoExtension.class)
class BillingServiceCreateInvoiceTest {

    @Mock InvoiceRepository         invoiceRepo;
    @Mock InvoiceNumberGenerator    numberGenerator;
    @Mock ReservationDetailPort     reservationDetail;
    @Mock InvoicePdfGenerator       pdfGenerator;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks BillingService service;

    // ── helpers ───────────────────────────────────────────────────────────

    private ReservationDetail buildDetail(ReservationId reservationId, long nights) {
        return new ReservationDetail(
                reservationId,
                "RES-2025-00001",
                GuestId.generate(),
                RoomId.generate(),
                "Budi Santoso",
                "3201010101800001",
                "+628111000001",
                "101",
                "Deluxe",
                LocalDate.now().minusDays(nights),
                LocalDate.now(),
                nights,
                Money.of(500_000L)
        );
    }

    private Invoice buildSavedInvoice(ReservationId reservationId, long nights) {
        return Invoice.create(
                "INV-2025-00001",
                reservationId,
                "RES-2025-00001",
                "Budi Santoso",
                nights,
                Money.of(500_000L),
                "Deluxe"
        );
    }

    // ══════════════════════════════════════════════════════════════
    // createInvoiceForCheckout()
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createInvoiceForCheckout()")
    class CreateInvoiceForCheckout {

        @Test
        @DisplayName("fetches reservation detail using the provided reservationId")
        void fetchesReservationDetail() {
            ReservationId reservationId = ReservationId.generate();
            given(reservationDetail.findById(reservationId))
                    .willReturn(buildDetail(reservationId, 3L));
            given(numberGenerator.generate()).willReturn("INV-2025-00001");
            given(invoiceRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            service.createInvoiceForCheckout(reservationId, 3L);

            then(reservationDetail).should().findById(reservationId);
        }

        @Test
        @DisplayName("uses the number generator to produce an invoice number")
        void usesNumberGenerator() {
            ReservationId reservationId = ReservationId.generate();
            given(reservationDetail.findById(any()))
                    .willReturn(buildDetail(reservationId, 2L));
            given(numberGenerator.generate()).willReturn("INV-2025-00042");
            given(invoiceRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            service.createInvoiceForCheckout(reservationId, 2L);

            then(numberGenerator).should().generate();
        }

        @Test
        @DisplayName("saves the created invoice exactly once")
        void savesInvoiceExactlyOnce() {
            ReservationId reservationId = ReservationId.generate();
            given(reservationDetail.findById(any()))
                    .willReturn(buildDetail(reservationId, 1L));
            given(numberGenerator.generate()).willReturn("INV-2025-00001");
            given(invoiceRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            service.createInvoiceForCheckout(reservationId, 1L);

            then(invoiceRepo).should(times(1)).save(any(Invoice.class));
        }

        @Test
        @DisplayName("saved invoice has UNPAID status (R-07: no premature payment event)")
        void savedInvoiceIsUnpaid() {
            ReservationId reservationId = ReservationId.generate();
            given(reservationDetail.findById(any()))
                    .willReturn(buildDetail(reservationId, 3L));
            given(numberGenerator.generate()).willReturn("INV-2025-00001");

            ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
            given(invoiceRepo.save(captor.capture())).willAnswer(inv -> inv.getArgument(0));

            service.createInvoiceForCheckout(reservationId, 3L);

            assertThat(captor.getValue().getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
        }

        @Test
        @DisplayName("does NOT publish PaymentReceivedEvent on invoice creation (R-07 fix)")
        void doesNotPublishPaymentEventOnCreation() {
            ReservationId reservationId = ReservationId.generate();
            given(reservationDetail.findById(any()))
                    .willReturn(buildDetail(reservationId, 3L));
            given(numberGenerator.generate()).willReturn("INV-2025-00001");
            given(invoiceRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            service.createInvoiceForCheckout(reservationId, 3L);

            then(eventPublisher).should(never()).publishEvent(any());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // recordPayment()
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("recordPayment()")
    class RecordPayment {

        @Test
        @DisplayName("throws ResourceNotFoundException for an unknown invoice ID")
        void throwsForUnknownInvoice() {
            InvoiceId unknownId = InvoiceId.generate();
            given(invoiceRepo.findById(unknownId)).willReturn(Optional.empty());

            RecordPaymentCommand cmd = new RecordPaymentCommand(
                    Money.of(500_000L).amount(), PaymentMethod.CASH, null, StaffId.generate());

            assertThatThrownBy(() -> service.recordPayment(unknownId, cmd))
                    .isInstanceOf(ResourceNotFoundException.class);

            then(invoiceRepo).should(never()).save(any());
            then(eventPublisher).should(never()).publishEvent(any());
        }

        @Test
        @DisplayName("saves the invoice after payment is recorded")
        void savesInvoiceAfterPayment() {
            ReservationId reservationId = ReservationId.generate();
            Invoice invoice = buildSavedInvoice(reservationId, 3L);
            InvoiceId invoiceId = invoice.getId();
            given(invoiceRepo.findById(invoiceId)).willReturn(Optional.of(invoice));
            given(invoiceRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            RecordPaymentCommand cmd = new RecordPaymentCommand(
                    Money.of(1_000_000L).amount(), PaymentMethod.CASH, null, StaffId.generate());

            service.recordPayment(invoiceId, cmd);

            then(invoiceRepo).should().save(invoice);
        }

        @Test
        @DisplayName("returns the updated InvoiceResponse")
        void returnsUpdatedResponse() {
            ReservationId reservationId = ReservationId.generate();
            Invoice invoice = buildSavedInvoice(reservationId, 3L);
            InvoiceId invoiceId = invoice.getId();
            given(invoiceRepo.findById(invoiceId)).willReturn(Optional.of(invoice));
            given(invoiceRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            RecordPaymentCommand cmd = new RecordPaymentCommand(
                    Money.of(1_665_000L).amount(), PaymentMethod.CASH, null, StaffId.generate());

            InvoiceResponse response = service.recordPayment(invoiceId, cmd);

            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("publishes PaymentReceivedEvent after payment")
        void publishesPaymentReceivedEvent() {
            ReservationId reservationId = ReservationId.generate();
            Invoice invoice = buildSavedInvoice(reservationId, 3L);
            InvoiceId invoiceId = invoice.getId();
            given(invoiceRepo.findById(invoiceId)).willReturn(Optional.of(invoice));
            given(invoiceRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            RecordPaymentCommand cmd = new RecordPaymentCommand(
                    Money.of(1_000_000L).amount(), PaymentMethod.CASH, null, StaffId.generate());

            service.recordPayment(invoiceId, cmd);

            then(eventPublisher).should().publishEvent(any(PaymentReceivedEvent.class));
        }

        @Test
        @DisplayName("publishes fullyPaid = true when invoice reaches PAID status")
        void publishesFullyPaidTrueWhenPaid() {
            ReservationId reservationId = ReservationId.generate();
            // 3 nights x 500,000 + 11% tax = 1,665,000 total
            Invoice invoice = buildSavedInvoice(reservationId, 3L);
            InvoiceId invoiceId = invoice.getId();
            given(invoiceRepo.findById(invoiceId)).willReturn(Optional.of(invoice));
            given(invoiceRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            RecordPaymentCommand fullPayment = new RecordPaymentCommand(
                    Money.of(1_665_000L).amount(), PaymentMethod.CASH, null, StaffId.generate());

            service.recordPayment(invoiceId, fullPayment);

            ArgumentCaptor<PaymentReceivedEvent> captor =
                    ArgumentCaptor.forClass(PaymentReceivedEvent.class);
            then(eventPublisher).should().publishEvent(captor.capture());
            assertThat(captor.getValue().isFullyPaid()).isTrue();
        }

        @Test
        @DisplayName("publishes fullyPaid = false when invoice is still PARTIAL")
        void publishesFullyPaidFalseWhenPartial() {
            ReservationId reservationId = ReservationId.generate();
            Invoice invoice = buildSavedInvoice(reservationId, 3L);
            InvoiceId invoiceId = invoice.getId();
            given(invoiceRepo.findById(invoiceId)).willReturn(Optional.of(invoice));
            given(invoiceRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            RecordPaymentCommand partialPayment = new RecordPaymentCommand(
                    Money.of(1_000_000L).amount(), PaymentMethod.CASH, null, StaffId.generate());

            service.recordPayment(invoiceId, partialPayment);

            ArgumentCaptor<PaymentReceivedEvent> captor =
                    ArgumentCaptor.forClass(PaymentReceivedEvent.class);
            then(eventPublisher).should().publishEvent(captor.capture());
            assertThat(captor.getValue().isFullyPaid()).isFalse();
        }

        @Test
        @DisplayName("payment with reference number is passed through correctly")
        void paymentWithReferenceNumber() {
            ReservationId reservationId = ReservationId.generate();
            Invoice invoice = buildSavedInvoice(reservationId, 2L);
            InvoiceId invoiceId = invoice.getId();
            given(invoiceRepo.findById(invoiceId)).willReturn(Optional.of(invoice));
            given(invoiceRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            RecordPaymentCommand cmd = new RecordPaymentCommand(
                    Money.of(500_000L).amount(),
                    PaymentMethod.BANK_TRANSFER,
                    "TRF-2025-00099",
                    StaffId.generate());

            assertThatNoException().isThrownBy(() -> service.recordPayment(invoiceId, cmd));
        }
    }
}
