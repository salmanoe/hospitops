-- ═══════════════════════════════════════════════════════════════
-- V22__backfill_hotel_name_in_hotel_summary.sql
--
-- V21 added hotel_name with DEFAULT '' and relied on the nightly
-- reconciliation job to populate names. New environments would show
-- hotel UUIDs on the group dashboard until the job ran.
--
-- This migration backfills hotel_name from the hotel table immediately,
-- so every hotel_summary row has a real name as soon as Flyway runs.
-- Rows already populated (hotel_name != '') are untouched — idempotent.
-- ═══════════════════════════════════════════════════════════════

UPDATE hotel_summary hs
SET hotel_name = h.name
FROM hotel h
WHERE hs.hotel_id = h.id
  AND hs.hotel_name = '';
