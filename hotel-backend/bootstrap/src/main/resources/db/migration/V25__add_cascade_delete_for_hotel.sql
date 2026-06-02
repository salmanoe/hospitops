-- ═══════════════════════════════════════════════════════════════
-- V25__add_cascade_delete_for_hotel.sql
--
-- WHY: Adds ON DELETE CASCADE to all hotel_id foreign keys that were
--      created in V13 (and V17, V19) without a delete action. Without
--      CASCADE, attempting to delete a hotel row produces an FK violation.
--
-- SAFETY: Application-level guard in HotelService.deleteHotel() rejects
--         deletion of any hotel not in SETUP status. CASCADE at the DB
--         level is the clean-up mechanism, not the safety gate.
--
-- Also adds CASCADE to room_rate_override.room_type_id so that when
-- room_type rows are cascade-deleted from hotel, their rate overrides
-- are removed too.
-- ═══════════════════════════════════════════════════════════════

-- ── room_type ────────────────────────────────────────────────────
ALTER TABLE room_type DROP CONSTRAINT room_type_hotel_id_fkey;
ALTER TABLE room_type ADD CONSTRAINT room_type_hotel_id_fkey
    FOREIGN KEY (hotel_id) REFERENCES hotel (id) ON DELETE CASCADE;

-- ── room_rate_override (child of room_type — cascades when room_type deleted) ─
ALTER TABLE room_rate_override DROP CONSTRAINT room_rate_override_room_type_id_fkey;
ALTER TABLE room_rate_override ADD CONSTRAINT room_rate_override_room_type_id_fkey
    FOREIGN KEY (room_type_id) REFERENCES room_type (id) ON DELETE CASCADE;

-- ── room ─────────────────────────────────────────────────────────
ALTER TABLE room DROP CONSTRAINT room_hotel_id_fkey;
ALTER TABLE room ADD CONSTRAINT room_hotel_id_fkey
    FOREIGN KEY (hotel_id) REFERENCES hotel (id) ON DELETE CASCADE;

-- ── staff ────────────────────────────────────────────────────────
ALTER TABLE staff DROP CONSTRAINT staff_hotel_id_fkey;
ALTER TABLE staff ADD CONSTRAINT staff_hotel_id_fkey
    FOREIGN KEY (hotel_id) REFERENCES hotel (id) ON DELETE CASCADE;

-- ── guest ────────────────────────────────────────────────────────
ALTER TABLE guest DROP CONSTRAINT guest_hotel_id_fkey;
ALTER TABLE guest ADD CONSTRAINT guest_hotel_id_fkey
    FOREIGN KEY (hotel_id) REFERENCES hotel (id) ON DELETE CASCADE;

-- ── reservation ──────────────────────────────────────────────────
ALTER TABLE reservation DROP CONSTRAINT reservation_hotel_id_fkey;
ALTER TABLE reservation ADD CONSTRAINT reservation_hotel_id_fkey
    FOREIGN KEY (hotel_id) REFERENCES hotel (id) ON DELETE CASCADE;

-- ── housekeeping_task ─────────────────────────────────────────────
ALTER TABLE housekeeping_task DROP CONSTRAINT housekeeping_task_hotel_id_fkey;
ALTER TABLE housekeeping_task ADD CONSTRAINT housekeeping_task_hotel_id_fkey
    FOREIGN KEY (hotel_id) REFERENCES hotel (id) ON DELETE CASCADE;

-- ── invoice ──────────────────────────────────────────────────────
ALTER TABLE invoice DROP CONSTRAINT invoice_hotel_id_fkey;
ALTER TABLE invoice ADD CONSTRAINT invoice_hotel_id_fkey
    FOREIGN KEY (hotel_id) REFERENCES hotel (id) ON DELETE CASCADE;

-- ── payment ──────────────────────────────────────────────────────
ALTER TABLE payment DROP CONSTRAINT payment_hotel_id_fkey;
ALTER TABLE payment ADD CONSTRAINT payment_hotel_id_fkey
    FOREIGN KEY (hotel_id) REFERENCES hotel (id) ON DELETE CASCADE;

-- ── hotel_summary ─────────────────────────────────────────────────
ALTER TABLE hotel_summary DROP CONSTRAINT hotel_summary_hotel_id_fkey;
ALTER TABLE hotel_summary ADD CONSTRAINT hotel_summary_hotel_id_fkey
    FOREIGN KEY (hotel_id) REFERENCES hotel (id) ON DELETE CASCADE;

-- ── hotel_policy_config ───────────────────────────────────────────
ALTER TABLE hotel_policy_config DROP CONSTRAINT hotel_policy_config_hotel_id_fkey;
ALTER TABLE hotel_policy_config ADD CONSTRAINT hotel_policy_config_hotel_id_fkey
    FOREIGN KEY (hotel_id) REFERENCES hotel (id) ON DELETE CASCADE;
