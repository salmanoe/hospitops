-- ═══════════════════════════════════════════════════════════════
-- V9__add_service_requested_room_status.sql
--
-- Adds SERVICE_REQUESTED to the room_status enum so that occupied
-- rooms can signal a mid-stay cleaning request without leaving
-- occupancy. The guest remains in the room; status returns to
-- OCCUPIED after housekeeping completes the service task.
--
-- Transition: OCCUPIED → SERVICE_REQUESTED → OCCUPIED
-- ═══════════════════════════════════════════════════════════════

ALTER TYPE room_status ADD VALUE IF NOT EXISTS 'SERVICE_REQUESTED';
