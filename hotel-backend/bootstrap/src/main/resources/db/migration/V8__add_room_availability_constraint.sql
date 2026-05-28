-- R-03 FIX: Prevent double-booking at the database level.
--
-- The existing composite index (idx_reservation_availability) speeds up
-- availability queries but does NOT prevent two concurrent transactions
-- from both passing the availability check and inserting overlapping
-- reservations for the same room — a classic TOCTOU race.
--
-- This migration adds a GIST exclusion constraint that atomically enforces
-- "no two non-cancelled reservations may overlap on the same room".
-- The application-level check (ReservationService.isAvailable) remains as
-- a fast first-line rejection for obviously unavailable dates; the DB
-- constraint is the authoritative guard against the race.
--
-- Requires the btree_gist extension (ships with standard PostgreSQL).

CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE reservation
    ADD CONSTRAINT uq_room_no_overlap
        EXCLUDE USING gist (
            room_id WITH =,
            daterange(check_in_date, check_out_date, '[)') WITH &&
        )
        WHERE (status NOT IN ('CANCELLED', 'CHECKED_OUT'));
