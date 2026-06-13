-- ═══════════════════════════════════════════════════════════════
-- V26__backfill_hotel_summary_revenue.sql
--
-- WHY: hotel_summary rows were seeded with revenue_today = 0 and
--      revenue_month = 0 (V17 DEFAULT). The incremental event handler
--      only updates these values for payments recorded AFTER the summary
--      row was created. The nightly reconciliation job (02:00) corrects
--      them, but until it runs the group dashboard shows Rp 0 for all
--      revenue KPIs on hotels that have existing payment history.
--
-- FIX: Compute revenue_today and revenue_month from the payment table
--      (joined to invoice for hotel_id) and update hotel_summary in one
--      pass. Idempotent — safe to run again; values are simply recomputed.
-- ═══════════════════════════════════════════════════════════════

UPDATE hotel_summary hs
SET
    revenue_today = COALESCE((
        SELECT SUM(p.amount)
        FROM payment p
        JOIN invoice i ON i.id = p.invoice_id
        WHERE i.hotel_id = hs.hotel_id
          AND DATE(p.paid_at) = CURRENT_DATE
    ), 0),

    revenue_month = COALESCE((
        SELECT SUM(p.amount)
        FROM payment p
        JOIN invoice i ON i.id = p.invoice_id
        WHERE i.hotel_id = hs.hotel_id
          AND DATE_TRUNC('month', p.paid_at) = DATE_TRUNC('month', CURRENT_DATE)
    ), 0);
