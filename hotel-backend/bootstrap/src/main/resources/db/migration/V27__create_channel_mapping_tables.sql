-- ═══════════════════════════════════════════════════════════════
-- V27 — Channel manager: provider mapping tables
--
-- Phase 2 (replace Bookandlink): maps HospitOps properties and room
-- types onto an external channel-connectivity provider (Channex) so
-- ARI can be pushed and OTA reservations pulled.
--
-- Multi-tenancy: every table carries hotel_id and cascades on hotel
-- delete (consistent with V25). Sync-state / outbox / inbound tables
-- are introduced by later slices alongside the code that uses them.
-- ═══════════════════════════════════════════════════════════════

-- ── One provider hookup per hotel ────────────────────────────────
-- external_property_id is the provider's identifier for this hotel
-- (e.g. a Channex property UUID). Credentials live in app config /
-- secrets, not here.
CREATE TABLE channel_property_mapping (
    id                   uuid         PRIMARY KEY,
    hotel_id             uuid         NOT NULL,
    provider             varchar(32)  NOT NULL DEFAULT 'CHANNEX',
    external_property_id varchar(128) NOT NULL,
    enabled              boolean      NOT NULL DEFAULT false,
    version              bigint       NOT NULL DEFAULT 0,
    created_at           timestamp    NOT NULL DEFAULT now(),
    updated_at           timestamp    NOT NULL DEFAULT now(),
    CONSTRAINT fk_channel_property_hotel
        FOREIGN KEY (hotel_id) REFERENCES hotel (id) ON DELETE CASCADE,
    -- One hookup per hotel per provider.
    CONSTRAINT uq_channel_property_hotel_provider
        UNIQUE (hotel_id, provider)
);

CREATE INDEX idx_channel_property_hotel_id ON channel_property_mapping (hotel_id);

-- ── Room-type ↔ provider rate-plan mapping ───────────────────────
-- Links a HospitOps room_type to the provider's room type + rate plan
-- so outbound ARI knows where to push and inbound bookings resolve to
-- the right room type.
CREATE TABLE channel_room_type_mapping (
    id                    uuid         PRIMARY KEY,
    hotel_id              uuid         NOT NULL,
    room_type_id          uuid         NOT NULL,
    external_room_type_id varchar(128) NOT NULL,
    external_rate_plan_id varchar(128) NOT NULL,
    version               bigint       NOT NULL DEFAULT 0,
    created_at            timestamp    NOT NULL DEFAULT now(),
    updated_at            timestamp    NOT NULL DEFAULT now(),
    CONSTRAINT fk_channel_rt_hotel
        FOREIGN KEY (hotel_id) REFERENCES hotel (id) ON DELETE CASCADE,
    CONSTRAINT fk_channel_rt_room_type
        FOREIGN KEY (room_type_id) REFERENCES room_type (id) ON DELETE CASCADE,
    -- A room type maps to exactly one provider room type.
    CONSTRAINT uq_channel_rt_hotel_room_type
        UNIQUE (hotel_id, room_type_id)
);

CREATE INDEX idx_channel_rt_hotel_id ON channel_room_type_mapping (hotel_id);
