-- ═══════════════════════════════════════════════════════════════
-- V3__backfill_invoice_items.sql
--
-- Backfills invoice_item rows for invoices created before the
-- persistence layer was fixed to write line items.
--
-- Root cause: InvoiceRepositoryImpl.toJpa() previously omitted the
-- items collection, so the invoice_item table was never populated.
-- All monetary data (subtotal, rate_per_night) was stored correctly
-- on the invoice/reservation rows, so this migration can reconstruct
-- every line item without data loss.
--
-- Logic mirrors Invoice.create() in the domain layer:
--   description  = room_type.name || ' — ' || nights || ' night(s)'
--   quantity     = check_out_date - check_in_date   (integer days)
--   unit_price   = reservation.rate_per_night       (locked at booking)
--   total_price  = invoice.subtotal                 (= quantity × unit_price)
--
-- Idempotent: the WHERE NOT EXISTS guard means re-running this
-- migration (or applying it after some invoices already have items)
-- will not create duplicates.
-- ═══════════════════════════════════════════════════════════════

INSERT INTO invoice_item (id, invoice_id, description, quantity, unit_price, total_price)
SELECT gen_random_uuid(),
       i.id,
       rt.name
           || ' — '
           || (r.check_out_date - r.check_in_date)
           || ' night(s)',
       (r.check_out_date - r.check_in_date),
       r.rate_per_night,
       i.subtotal
FROM invoice i
         JOIN reservation r  ON r.id  = i.reservation_id
         JOIN room         rm ON rm.id = r.room_id
         JOIN room_type    rt ON rt.id = rm.room_type_id
WHERE NOT EXISTS (
    SELECT 1 FROM invoice_item ii WHERE ii.invoice_id = i.id
);
