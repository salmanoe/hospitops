-- ═══════════════════════════════════════════════════════════════
-- V11__create_hotel_table.sql
-- Phase 2: Multi-hotel — hotel lifecycle table
--
-- hotel_status: SETUP → ACTIVE → SUSPENDED
-- Hotels start in SETUP and cannot receive reservations until ACTIVE.
-- The @Version column provides optimistic locking for status transitions.
-- ═══════════════════════════════════════════════════════════════

CREATE TYPE hotel_status AS ENUM ('SETUP', 'ACTIVE', 'SUSPENDED');

CREATE TABLE hotel
(
    id                      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    -- Cross-module reference to group — real FK in Stage 1. [tech-debt]
    group_id                UUID         NOT NULL REFERENCES "group" (id),
    name                    VARCHAR(200) NOT NULL,
    address                 TEXT,
    timezone                VARCHAR(50)  NOT NULL DEFAULT 'UTC',
    currency                CHAR(3)      NOT NULL DEFAULT 'IDR',
    star_rating             INT          NOT NULL DEFAULT 3
                                         CHECK (star_rating BETWEEN 1 AND 5),
    default_check_in_time   TIME         NOT NULL DEFAULT '14:00',
    default_check_out_time  TIME         NOT NULL DEFAULT '12:00',
    status                  hotel_status NOT NULL DEFAULT 'SETUP',
    -- Optimistic locking: status transitions are concurrent-sensitive
    version                 BIGINT       NOT NULL DEFAULT 0,
    created_at              TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_hotel_group_id ON hotel (group_id);
CREATE INDEX idx_hotel_status   ON hotel (status);
