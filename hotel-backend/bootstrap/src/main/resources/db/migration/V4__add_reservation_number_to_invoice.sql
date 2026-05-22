-- ═══════════════════════════════════════════════════════════════
-- V4__add_reservation_number_to_invoice.sql
--
-- Adds the reservation_number column to the invoice table so the
-- billing list can display the human-readable booking code
-- (e.g. RES-2024-001) without a cross-module lookup on every page.
--
-- The column is populated from the reservation table for all
-- existing rows. New invoices will have it set at creation time
-- by BillingService.createInvoiceForCheckout().
-- ═══════════════════════════════════════════════════════════════

ALTER TABLE invoice
    ADD COLUMN reservation_number VARCHAR(20);

UPDATE invoice i
SET reservation_number = r.reservation_number
FROM reservation r
WHERE r.id = i.reservation_id;

ALTER TABLE invoice
    ALTER COLUMN reservation_number SET NOT NULL;
