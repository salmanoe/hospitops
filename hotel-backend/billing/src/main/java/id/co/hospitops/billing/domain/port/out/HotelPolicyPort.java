package id.co.hospitops.billing.domain.port.out;

import id.co.hospitops.shared.HotelId;

/**
 * Anti-corruption port that gives the billing domain read-only access to
 * hotel policy configuration without coupling to the {@code hotel} module's
 * internals.
 *
 * <p>Implemented in {@code bootstrap} by {@code HotelPolicyAdapter}, which
 * reads from the {@code hotel_policy_config} table. If no policy has been
 * configured for the hotel, the adapter returns safe defaults so that
 * invoice generation never fails for existing hotels migrated before
 * Phase 9 (V19 seeds a default row for the initial hotel).
 */
public interface HotelPolicyPort {

    /**
     * Carries the invoice-relevant subset of a hotel's policy config.
     * Only primitives and strings — no domain objects cross module boundaries.
     *
     * @param invoiceHotelName  hotel name to print in the invoice header
     * @param invoiceAddress    optional hotel address (maybe null)
     * @param invoiceFooterNote optional closing note printed at invoice bottom (maybe null)
     * @param taxPercent        tax rate as an integer percentage, 0–100
     * @param taxName           tax label printed on the invoice, e.g. "PPN", "VAT"
     */
    record HotelPolicy(
            String invoiceHotelName,
            String invoiceAddress,
            String invoiceFooterNote,
            int taxPercent,
            String taxName
    ) {}

    /**
     * Returns the policy for the given hotel. Never returns {@code null};
     * implementations must provide a default if no policy has been persisted.
     */
    HotelPolicy findByHotelId(HotelId hotelId);
}
