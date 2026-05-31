package id.co.hospitops.hotel.domain.model;

import id.co.hospitops.shared.Guard;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.PolicyConfigId;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Holds the tax and invoice branding configuration for a hotel.
 *
 * <p>Lifecycle: created on first save, updated in place (upsert). One record per hotel.
 *
 * <p>Saving a {@code PolicyConfig} for a hotel in {@code SETUP} status automatically
 * marks the {@link SetupStep#POLICY} wizard step complete — see
 * {@link id.co.hospitops.hotel.application.HotelPolicyService}.
 */
@Getter
public class PolicyConfig {

    private final PolicyConfigId id;
    private final HotelId hotelId;

    /** Tax rate as an integer percentage, 0–100. 0 means tax-exempt. */
    private int taxPercent;

    /**
     * Human-readable tax label printed on invoices (e.g. "PPN", "VAT", "GST").
     * Required even when {@code taxPercent} is 0 — use a value like "Tax-Exempt".
     */
    private String taxName;

    /** Hotel name printed in the invoice header. Defaults to the hotel's own name if blank. */
    private String invoiceHotelName;

    /** Optional hotel address printed below the header on invoices. */
    private String invoiceAddress;

    /** Optional closing note at the bottom of the invoice (e.g. "Thank you for your stay."). */
    private String invoiceFooterNote;

    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PolicyConfig create(HotelId hotelId,
                                      int taxPercent, String taxName,
                                      String invoiceHotelName, String invoiceAddress,
                                      String invoiceFooterNote) {
        Guard.notNull(hotelId, "HotelId");
        validateTaxPercent(taxPercent);
        Guard.notBlank(taxName, "Tax name");
        Guard.notBlank(invoiceHotelName, "Invoice hotel name");
        LocalDateTime now = LocalDateTime.now();
        return new PolicyConfig(PolicyConfigId.generate(), hotelId,
                taxPercent, taxName.trim(), invoiceHotelName.trim(),
                invoiceAddress, invoiceFooterNote, now, now);
    }

    public static PolicyConfig reconstitute(PolicyConfigId id, HotelId hotelId,
                                            int taxPercent, String taxName,
                                            String invoiceHotelName, String invoiceAddress,
                                            String invoiceFooterNote,
                                            LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new PolicyConfig(id, hotelId, taxPercent, taxName, invoiceHotelName,
                invoiceAddress, invoiceFooterNote, createdAt, updatedAt);
    }

    private PolicyConfig(PolicyConfigId id, HotelId hotelId,
                         int taxPercent, String taxName,
                         String invoiceHotelName, String invoiceAddress,
                         String invoiceFooterNote,
                         LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.hotelId = hotelId;
        this.taxPercent = taxPercent;
        this.taxName = taxName;
        this.invoiceHotelName = invoiceHotelName;
        this.invoiceAddress = invoiceAddress;
        this.invoiceFooterNote = invoiceFooterNote;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Applies updated policy fields in place (upsert pattern).
     * The {@code hotelId} and {@code id} are immutable.
     */
    public void update(int taxPercent, String taxName,
                       String invoiceHotelName, String invoiceAddress,
                       String invoiceFooterNote) {
        validateTaxPercent(taxPercent);
        Guard.notBlank(taxName, "Tax name");
        Guard.notBlank(invoiceHotelName, "Invoice hotel name");
        this.taxPercent = taxPercent;
        this.taxName = taxName.trim();
        this.invoiceHotelName = invoiceHotelName.trim();
        this.invoiceAddress = invoiceAddress;
        this.invoiceFooterNote = invoiceFooterNote;
        this.updatedAt = LocalDateTime.now();
    }

    private static void validateTaxPercent(int percent) {
        if (percent < 0 || percent > 100) {
            throw new id.co.hospitops.shared.exception.BusinessRuleViolationException(
                    "Tax percent must be between 0 and 100, got: " + percent);
        }
    }
}
