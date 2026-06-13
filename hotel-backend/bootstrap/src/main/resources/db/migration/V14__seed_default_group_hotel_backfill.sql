-- ═══════════════════════════════════════════════════════════════
-- V14__seed_default_group_hotel_backfill.sql
-- Phase 4: Multi-hotel — data migration
--
-- This migration:
--   1. Creates the default Group for the single existing hotel
--   2. Creates the default GROUP_ADMIN account
--   3. Creates the default Hotel record (ACTIVE — already live)
--   4. Creates the setup checklist row (all steps complete)
--   5. Backfills hotel_id on ALL existing rows in every hotel-scoped table
--
-- WHY FIXED UUIDs:
--   Fixed UUIDs allow future migrations and integration tests to reference
--   these seed records by a known ID rather than a dynamic lookup.
--
-- DEFAULT CREDENTIALS (GROUP_ADMIN):
--   Email:    groupadmin@hospitops.local
--   Password: admin123  (BCrypt strength 12 — same as staff seed in V2)
--   ⚠ Change before deploying to any shared environment:
--     PATCH /api/v1/group/admin/password  (Phase 6+)
--
-- ROLLBACK PLAN (before Phase 5 — while hotel_id is still nullable):
--   UPDATE staff             SET hotel_id = NULL;
--   UPDATE room_type         SET hotel_id = NULL;
--   UPDATE room              SET hotel_id = NULL;
--   UPDATE guest             SET hotel_id = NULL;
--   UPDATE reservation       SET hotel_id = NULL;
--   UPDATE housekeeping_task SET hotel_id = NULL;
--   UPDATE invoice           SET hotel_id = NULL;
--   UPDATE payment           SET hotel_id = NULL;
--   DELETE FROM hotel_setup_checklist WHERE hotel_id = 'b0000000-0000-0000-0000-000000000002';
--   DELETE FROM hotel        WHERE id = 'b0000000-0000-0000-0000-000000000002';
--   DELETE FROM group_admin  WHERE id = 'b0000000-0000-0000-0000-000000000003';
--   DELETE FROM "group"      WHERE id = 'b0000000-0000-0000-0000-000000000001';
-- ═══════════════════════════════════════════════════════════════

DO $$
DECLARE
    v_group_id UUID := 'b0000000-0000-0000-0000-000000000001';
    v_hotel_id UUID := 'b0000000-0000-0000-0000-000000000002';
    v_admin_id UUID := 'b0000000-0000-0000-0000-000000000003';
BEGIN

    -- ── 1. Default Group ──────────────────────────────────────────────
    INSERT INTO "group" (id, name, owner_email, created_at, updated_at)
    VALUES (
        v_group_id,
        'Default Group',
        'groupadmin@hospitops.local',
        NOW(),
        NOW()
    );

    -- ── 2. Default GROUP_ADMIN ────────────────────────────────────────
    -- Password: admin123  (BCrypt strength 12, $2b$ prefix)
    -- Same hashing standard as V2 staff seed. Change before production.
    INSERT INTO group_admin (id, group_id, email, password_hash, created_at, updated_at)
    VALUES (
        v_admin_id,
        v_group_id,
        'groupadmin@hospitops.local',
        '$2b$12$te0zOtNd9lbtvEH2sYLWJukOXbYBQ0xSKr4RG9f2cCc.2NCjGoOFW',
        NOW(),
        NOW()
    );

    -- ── 3. Default Hotel (already live — ACTIVE, not SETUP) ───────────
    INSERT INTO hotel (
        id, group_id, name, address, timezone, currency,
        star_rating, default_check_in_time, default_check_out_time,
        status, version, created_at, updated_at
    )
    VALUES (
        v_hotel_id,
        v_group_id,
        'Default Hotel',
        NULL,           -- address to be filled in by operator
        'UTC',
        'IDR',
        3,
        '14:00',
        '12:00',
        'ACTIVE',
        0,
        NOW(),
        NOW()
    );

    -- ── 4. Setup checklist — all steps complete (hotel is already live) ─
    INSERT INTO hotel_setup_checklist (
        hotel_id,
        profile_complete, policy_complete,
        room_type_added, room_added, staff_account_created,
        updated_at
    )
    VALUES (
        v_hotel_id,
        TRUE, TRUE, TRUE, TRUE, TRUE,
        NOW()
    );

    -- ── 5. Backfill hotel_id on all existing rows ─────────────────────
    -- Every row that exists before this migration belongs to the single
    -- default hotel. Rows created after this migration will carry
    -- hotel_id from the application layer (Phase 5+).

    UPDATE staff             SET hotel_id = v_hotel_id WHERE hotel_id IS NULL;
    UPDATE room_type         SET hotel_id = v_hotel_id WHERE hotel_id IS NULL;
    UPDATE room              SET hotel_id = v_hotel_id WHERE hotel_id IS NULL;
    UPDATE guest             SET hotel_id = v_hotel_id WHERE hotel_id IS NULL;
    UPDATE reservation       SET hotel_id = v_hotel_id WHERE hotel_id IS NULL;
    UPDATE housekeeping_task SET hotel_id = v_hotel_id WHERE hotel_id IS NULL;
    UPDATE invoice           SET hotel_id = v_hotel_id WHERE hotel_id IS NULL;
    UPDATE payment           SET hotel_id = v_hotel_id WHERE hotel_id IS NULL;

END $$;
