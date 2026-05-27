package id.co.hospitops.billing.adapter.web;

// R-15 FIX: Removed direct import of id.co.hospitops.identity.domain.model.Staff.
// Same fix as R-14 in ReservationController — use @AuthenticationPrincipal with
// expression = "id" to extract only the StaffId (a shared-kernel type) from the
// security principal, keeping the billing module boundary clean.

import id.co.hospitops.billing.application.command.RecordPaymentCommand;
import id.co.hospitops.billing.application.response.InvoiceResponse;
import id.co.hospitops.billing.domain.port.in.BillingUseCase;
import id.co.hospitops.shared.InvoiceId;
import id.co.hospitops.shared.StaffId;
import id.co.hospitops.shared.web.ApiResponse;
import id.co.hospitops.shared.web.PageResult;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class BillingController {

    private final BillingUseCase billingUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<InvoiceResponse>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(billingUseCase.findAll(status,
                PageRequest.of(page, size, Sort.by("issuedAt").descending()))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(billingUseCase.findById(InvoiceId.of(id))));
    }

    // R3-02 FIX: Narrowed throws clause from Exception to IOException.
    // The only checked exception reachable here is from response.getOutputStream().write();
    // InvoicePdfGenerator wraps iText 8 exceptions as unchecked runtime exceptions.
    @GetMapping("/{id}/pdf")
    public void downloadPdf(@PathVariable UUID id,
                            HttpServletResponse response) throws IOException {
        byte[] pdf = billingUseCase.generatePdf(InvoiceId.of(id));
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=invoice-" + id + ".pdf");
        response.setContentLength(pdf.length);
        response.getOutputStream().write(pdf);
    }

    @PostMapping("/{id}/payments")
    @SuppressWarnings({"JvmTaintAnalysis", "SpringElInspection"}) // @Valid + Bean Validation; UUID path var is type-safe; JSON response; SpEL 'id' resolves at runtime on the Staff principal
    public ResponseEntity<ApiResponse<InvoiceResponse>> recordPayment(
            @PathVariable UUID id,
            @Valid @RequestBody RecordPaymentRequest req,
            // R-15 FIX: expression = "id" extracts StaffId directly — no identity
            // module import needed. Same pattern as R-14 in ReservationController.
            @AuthenticationPrincipal(expression = "id") StaffId staffId) {
        var cmd = new RecordPaymentCommand(
                req.amount(), req.method(), req.referenceNo(), staffId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(billingUseCase.recordPayment(InvoiceId.of(id), cmd)));
    }
}
