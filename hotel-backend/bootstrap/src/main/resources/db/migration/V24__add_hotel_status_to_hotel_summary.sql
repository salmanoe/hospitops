-- ═══════════════════════════════════════════════════════════════
-- V24__add_hotel_status_to_hotel_summary.sql
--
-- WHY: The group dashboard showed an "Enter →" button for all hotels
--      regardless of lifecycle status. A hotel in SETUP cannot be entered
--      for normal operations — it has no rooms, no staff, and the hotel
--      context would be incomplete. The fix requires the dashboard to know
--      each hotel's status so it can show "Complete Setup →" instead.
--
-- Backfill: join to hotel.status so existing rows get the correct value.
-- ═══════════════════════════════════════════════════════════════

ALTER TABLE hotel_summary
    ADD COLUMN hotel_status VARCHAR(20) NOT NULL DEFAULT 'SETUP';

UPDATE hotel_summary hs
SET hotel_status = h.status
FROM hotel h
WHERE h.id = hs.hotel_id;
