-- Add hotel_name to hotel_summary so the group dashboard can display names
-- without a secondary join against the hotel table.
-- DEFAULT '' handles existing rows; the nightly reconciliation job will
-- backfill real names on its next run.
ALTER TABLE hotel_summary
    ADD COLUMN hotel_name VARCHAR(200) NOT NULL DEFAULT '';
