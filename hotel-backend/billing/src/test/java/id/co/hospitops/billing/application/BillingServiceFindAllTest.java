package id.co.hospitops.billing.application;

import id.co.hospitops.billing.domain.model.PaymentStatus;
import id.co.hospitops.billing.domain.port.out.HotelPolicyPort;
import id.co.hospitops.billing.domain.port.out.InvoiceNumberGenerator;
import id.co.hospitops.billing.domain.port.out.InvoiceRepository;
import id.co.hospitops.billing.domain.port.out.ReservationDetailPort;
import id.co.hospitops.billing.infrastructure.pdf.InvoicePdfGenerator;
import id.co.hospitops.shared.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BillingService.findAll() — R3-05 fix.
 * <p>
 * Verifies that unknown PaymentStatus strings produce a clean HTTP-400-friendly
 * BusinessRuleViolationException instead of a raw IllegalArgumentException (500).
 * Also covers valid status filter and no-filter paths.
 */
@DisplayName("BillingService.findAll()")
@ExtendWith(MockitoExtension.class)
class BillingServiceFindAllTest {

    @Mock
    InvoiceRepository invoiceRepo;
    @Mock
    InvoiceNumberGenerator numberGenerator;
    @Mock
    ReservationDetailPort reservationDetail;
    @Mock
    HotelPolicyPort hotelPolicyPort;
    @Mock
    InvoicePdfGenerator pdfGenerator;
    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    BillingService service;

    private final Pageable page = PageRequest.of(0, 10);

    // ── Invalid status ─────────────────────────────────────────────

    @Nested
    @DisplayName("invalid status filter")
    class InvalidStatus {

        @ParameterizedTest(name = "statusFilter = \"{0}\"")
        @ValueSource(strings = {"SETTLED", "OVERDUE", "pending", "UNKNOWN", "123"})
        @DisplayName("throws BusinessRuleViolationException for unknown status values")
        void throwsForUnknownStatus(String badStatus) {
            assertThatThrownBy(() -> service.findAll(badStatus, page))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining(badStatus);
        }

        @Test
        @DisplayName("exception message includes the offending value")
        void exceptionMessageIncludesOffendingValue() {
            assertThatThrownBy(() -> service.findAll("GARBAGE", page))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("GARBAGE");
        }

        @Test
        @DisplayName("never reaches the repository on invalid status")
        void doesNotCallRepoOnInvalidStatus() {
            assertThatThrownBy(() -> service.findAll("INVALID", page))
                    .isInstanceOf(BusinessRuleViolationException.class);
            verifyNoInteractions(invoiceRepo);
        }
    }

    // ── No filter ──────────────────────────────────────────────────

    @Nested
    @DisplayName("no status filter")
    class NoFilter {

        @Test
        @DisplayName("null statusFilter returns all invoices")
        void nullFilterReturnsAll() {
            when(invoiceRepo.findAll(page)).thenReturn(List.of());
            when(invoiceRepo.count()).thenReturn(0L);

            var result = service.findAll(null, page);

            assertThat(result.content()).isEmpty();
            verify(invoiceRepo).findAll(page);
            verify(invoiceRepo, never()).findByStatus(any(), any());
        }

        @Test
        @DisplayName("blank statusFilter also returns all invoices")
        void blankFilterReturnsAll() {
            when(invoiceRepo.findAll(page)).thenReturn(List.of());
            when(invoiceRepo.count()).thenReturn(0L);

            var result = service.findAll("   ", page);

            assertThat(result.content()).isEmpty();
            verify(invoiceRepo).findAll(page);
            verify(invoiceRepo, never()).findByStatus(any(), any());
        }
    }

    // ── Valid status ───────────────────────────────────────────────

    @Nested
    @DisplayName("valid status filter")
    class ValidStatus {

        @ParameterizedTest(name = "status = {0}")
        @ValueSource(strings = {"UNPAID", "PAID", "PARTIAL"})
        @DisplayName("delegates to findByStatus() for known values")
        void delegatesToFindByStatus(String validStatus) {
            when(invoiceRepo.findByStatus(any(), eq(page))).thenReturn(List.of());
            when(invoiceRepo.count()).thenReturn(0L);

            var result = service.findAll(validStatus, page);

            assertThat(result).isNotNull();
            verify(invoiceRepo).findByStatus(PaymentStatus.valueOf(validStatus), page);
            verify(invoiceRepo, never()).findAll(page);
        }

        @Test
        @DisplayName("status filter is case-insensitive (lowercase input)")
        void caseInsensitiveFilter() {
            when(invoiceRepo.findByStatus(eq(PaymentStatus.PAID), eq(page))).thenReturn(List.of());
            when(invoiceRepo.count()).thenReturn(0L);

            assertThatNoException().isThrownBy(() -> service.findAll("paid", page));
            verify(invoiceRepo).findByStatus(PaymentStatus.PAID, page);
        }
    }
}
