-- ═══════════════════════════════════════════════════════════════
-- V28 — Channel manager: outbox for outbound ARI sync
--
-- Transactional outbox. Hotel-scoped writes that change availability or
-- rates enqueue a message here in the same transaction; a background
-- relay delivers it to the provider with at-least-once retries. Provider
-- ARI updates are idempotent, so re-delivery is safe.
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE channel_sync_message (
    id              uuid         PRIMARY KEY,
    hotel_id        uuid         NOT NULL,
    type            varchar(32)  NOT NULL,
    payload         text         NOT NULL,
    status          varchar(16)  NOT NULL DEFAULT 'PENDING',
    attempts        int          NOT NULL DEFAULT 0,
    last_error      text,
    next_attempt_at timestamp    NOT NULL DEFAULT now(),
    created_at      timestamp    NOT NULL DEFAULT now(),
    updated_at      timestamp    NOT NULL DEFAULT now(),
    CONSTRAINT fk_channel_sync_hotel
        FOREIGN KEY (hotel_id) REFERENCES hotel (id) ON DELETE CASCADE
);

-- Drives the relay's "due work" query: PENDING rows whose retry time has arrived.
CREATE INDEX idx_channel_sync_due ON channel_sync_message (status, next_attempt_at);
CREATE INDEX idx_channel_sync_hotel_id ON channel_sync_message (hotel_id);
