-- ═══════════════════════════════════════════════════════════════
-- V12__create_hotel_setup_checklist_table.sql
-- Phase 2: Multi-hotel — setup wizard checklist
--
-- One row per hotel (hotel_id is both PK and FK).
-- Tracks which of the 5 setup steps have been completed.
-- When all five are TRUE, HotelService auto-transitions status → ACTIVE.
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE hotel_setup_checklist
(
    hotel_id              UUID    PRIMARY KEY REFERENCES hotel (id) ON DELETE CASCADE,
    profile_complete      BOOLEAN NOT NULL DEFAULT FALSE,
    policy_complete       BOOLEAN NOT NULL DEFAULT FALSE,
    room_type_added       BOOLEAN NOT NULL DEFAULT FALSE,
    room_added            BOOLEAN NOT NULL DEFAULT FALSE,
    staff_account_created BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at            TIMESTAMP        DEFAULT NOW()
);
