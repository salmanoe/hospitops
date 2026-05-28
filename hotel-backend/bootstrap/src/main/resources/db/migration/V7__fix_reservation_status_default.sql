-- R-06 FIX: Align DB default with application behaviour.
--
-- Reservation.create() always sets status = CONFIRMED, but the column
-- default was PENDING — a silent mismatch that could produce PENDING rows
-- if a reservation were ever inserted without an explicit status value
-- (e.g. via a raw SQL script, migration backfill, or future tool).
--
-- PENDING is retained as a valid enum value for any legacy rows and to keep
-- the state-machine complete, but new rows should default to CONFIRMED.

ALTER TABLE reservation
    ALTER COLUMN status SET DEFAULT 'CONFIRMED';
