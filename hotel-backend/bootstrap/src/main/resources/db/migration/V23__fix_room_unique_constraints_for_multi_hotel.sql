-- ═══════════════════════════════════════════════════════════════
-- V23__fix_room_unique_constraints_for_multi_hotel.sql
--
-- WHY: V1 defined globally-unique constraints on room_type.name and
--      room.room_number. After multi-hotel was introduced (V13), these
--      constraints became incorrect: every hotel should be able to have
--      its own "Standard" room type or room "101". Two hotels colliding
--      on these names produces a duplicate-key 500.
--
-- FIX: Drop the global unique constraints and replace them with
--      composite unique constraints scoped to hotel_id.
-- ═══════════════════════════════════════════════════════════════

-- room_type: name must be unique per hotel, not globally
ALTER TABLE room_type
    DROP CONSTRAINT IF EXISTS room_type_name_key;

ALTER TABLE room_type
    ADD CONSTRAINT uq_room_type_name_hotel UNIQUE (name, hotel_id);

-- room: room_number must be unique per hotel, not globally
ALTER TABLE room
    DROP CONSTRAINT IF EXISTS room_room_number_key;

ALTER TABLE room
    ADD CONSTRAINT uq_room_number_hotel UNIQUE (room_number, hotel_id);

-- Optimistic locking: add @Version columns (architecture contract §DB rules)
ALTER TABLE room_type
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE room
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
