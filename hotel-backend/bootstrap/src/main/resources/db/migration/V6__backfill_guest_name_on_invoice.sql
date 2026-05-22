-- ═══════════════════════════════════════════════════════════════
-- V6__backfill_guest_name_on_invoice.sql
--
-- Populates the guest_name column added in V5 for all existing
-- invoice rows by joining invoice → reservation → guest.
-- New invoices will have guest_name set at creation time via
-- BillingService.createInvoiceForCheckout().
-- ═══════════════════════════════════════════════════════════════

UPDATE invoice i
SET guest_name = g.full_name
FROM reservation r
         JOIN guest g ON g.id = r.guest_id
WHERE r.id = i.reservation_id;

ALTER TABLE invoice
    ALTER COLUMN guest_name SET NOT NULL;
