package id.co.hospitops.hotel.adapter.web.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating or updating a hotel's policy configuration.
 *
 * @param taxPercent        tax rate as an integer percentage, 0–100 (0 = tax-exempt)
 * @param taxName           label shown on invoices, e.g. "PPN", "VAT", "GST"
 * @param invoiceHotelName  hotel name to print in the invoice header
 * @param invoiceAddress    optional hotel address for the invoice (maybe null)
 * @param invoiceFooterNote optional closing note at the bottom of every invoice
 */
public record SavePolicyConfigRequest(

        @Min(value = 0, message = "Tax percent must be at least 0")
        @Max(value = 100, message = "Tax percent must be at most 100")
        int taxPercent,

        @NotBlank(message = "Tax name is required")
        @Size(max = 50, message = "Tax name must be at most 50 characters")
        String taxName,

        @NotBlank(message = "Invoice hotel name is required")
        @Size(max = 200, message = "Invoice hotel name must be at most 200 characters")
        String invoiceHotelName,

        String invoiceAddress,

        String invoiceFooterNote
) {}
