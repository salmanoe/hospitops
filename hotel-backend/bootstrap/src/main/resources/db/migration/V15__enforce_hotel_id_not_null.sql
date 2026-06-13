-- ═══════════════════════════════════════════════════════════════
-- V15__enforce_hotel_id_not_null.sql
-- Phase 5: Multi-hotel — enforce hotel_id NOT NULL on all hotel-scoped tables
--
-- PREREQUISITE: V14 must have backfilled hotel_id on all existing rows.
-- Run the row-count verification query below before applying in production:
--
--   SELECT tbl, COUNT(*) AS nulls FROM (
--     SELECT 'staff'             AS tbl, hotel_id FROM staff
--     UNION ALL SELECT 'room_type',      hotel_id FROM room_type
--     UNION ALL SELECT 'room',           hotel_id FROM room
--     UNION ALL SELECT 'guest',          hotel_id FROM guest
--     UNION ALL SELECT 'reservation',    hotel_id FROM reservation
--     UNION ALL SELECT 'housekeeping_task', hotel_id FROM housekeeping_task
--     UNION ALL SELECT 'invoice',        hotel_id FROM invoice
--     UNION ALL SELECT 'payment',        hotel_id FROM payment
--   ) t WHERE hotel_id IS NULL GROUP BY tbl;
--   -- Expected: zero rows returned.
--
-- ROLLBACK (if needed — removes NOT NULL constraint only, data is preserved):
--   ALTER TABLE staff             ALTER COLUMN hotel_id DROP NOT NULL;
--   ALTER TABLE room_type         ALTER COLUMN hotel_id DROP NOT NULL;
--   ALTER TABLE room              ALTER COLUMN hotel_id DROP NOT NULL;
--   ALTER TABLE guest             ALTER COLUMN hotel_id DROP NOT NULL;
--   ALTER TABLE reservation       ALTER COLUMN hotel_id DROP NOT NULL;
--   ALTER TABLE housekeeping_task ALTER COLUMN hotel_id DROP NOT NULL;
--   ALTER TABLE invoice           ALTER COLUMN hotel_id DROP NOT NULL;
--   ALTER TABLE payment           ALTER COLUMN hotel_id DROP NOT NULL;
-- ═══════════════════════════════════════════════════════════════

ALTER TABLE staff             ALTER COLUMN hotel_id SET NOT NULL;
ALTER TABLE room_type         ALTER COLUMN hotel_id SET NOT NULL;
ALTER TABLE room              ALTER COLUMN hotel_id SET NOT NULL;
ALTER TABLE guest             ALTER COLUMN hotel_id SET NOT NULL;
ALTER TABLE reservation       ALTER COLUMN hotel_id SET NOT NULL;
ALTER TABLE housekeeping_task ALTER COLUMN hotel_id SET NOT NULL;
ALTER TABLE invoice           ALTER COLUMN hotel_id SET NOT NULL;
ALTER TABLE payment           ALTER COLUMN hotel_id SET NOT NULL;
