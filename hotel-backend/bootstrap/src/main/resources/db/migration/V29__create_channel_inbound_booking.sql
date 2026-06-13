-- ═══════════════════════════════════════════════════════════════
-- V29 — Channel manager: inbound OTA booking idempotency
--
-- Links a provider booking (stable external_booking_id) to the HospitOps
-- reservation it produced, so re-served revisions never double-book. Also
-- records CONFLICT outcomes (no room / unmapped type) for staff follow-up.
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE channel_inbound_booking (
    id                   uuid         PRIMARY KEY,
    hotel_id             uuid         NOT NULL,
    external_booking_id  varchar(128) NOT NULL,
    -- No FK to reservation (different module's table); may be null on CONFLICT.
    reservation_id       uuid,
    last_revision_id     varchar(128),
    status               varchar(16)  NOT NULL,
    ota_name             varchar(64),
    ota_reservation_code varchar(128),
    created_at           timestamp    NOT NULL DEFAULT now(),
    updated_at           timestamp    NOT NULL DEFAULT now(),
    CONSTRAINT fk_channel_inbound_hotel
        FOREIGN KEY (hotel_id) REFERENCES hotel (id) ON DELETE CASCADE,
    CONSTRAINT uq_channel_inbound_hotel_booking
        UNIQUE (hotel_id, external_booking_id)
);

CREATE INDEX idx_channel_inbound_hotel_id ON channel_inbound_booking (hotel_id);
