-- ═══════════════════════════════════════════════════════════════
-- V13__add_hotel_id_nullable.sql
-- Phase 3: Multi-hotel — add hotel_id to all hotel-scoped tables
--
-- hotel_id is added as a NULLABLE FK to hotel(id).
--
-- WHY NULLABLE at this stage:
--   All existing rows belong to the single hotel that already exists
--   in production. Making it NOT NULL here would require a simultaneous
--   backfill — which is done in the next phase (V14 — data migration).
--   Nullable now → safe rollback → enforced in V15.
--
-- WHY NO APPLICATION CODE CHANGES:
--   Hibernate ddl-auto=validate only checks that mapped columns exist
--   in the DB. It does NOT fail for unmapped columns. hotel_id lives
--   in the DB here but is not yet mapped in any JPA @Entity. The JPA
--   mapping lands in Phase 5 alongside NOT NULL enforcement.
--
-- Cross-module FK note (ARCHITECTURE.md §5):
--   These are real FK constraints in Stage 1 (monolith).
--   In Stage 3 (microservices) they become logical application-level
--   references — document as technical debt for each FK added here.
-- ═══════════════════════════════════════════════════════════════

-- ── identity module ──────────────────────────────────────────────
-- tech-debt: staff.hotel_id → hotel.id — FK to drop before identity
--            module is extracted into its own service in Stage 3.
ALTER TABLE staff
    ADD COLUMN hotel_id UUID REFERENCES hotel(id);

CREATE INDEX idx_staff_hotel_id ON staff (hotel_id);

-- ── room module ──────────────────────────────────────────────────
-- tech-debt: room_type.hotel_id → hotel.id
ALTER TABLE room_type
    ADD COLUMN hotel_id UUID REFERENCES hotel(id);

CREATE INDEX idx_room_type_hotel_id ON room_type (hotel_id);

-- tech-debt: room.hotel_id → hotel.id
ALTER TABLE room
    ADD COLUMN hotel_id UUID REFERENCES hotel(id);

CREATE INDEX idx_room_hotel_id ON room (hotel_id);

-- ── guest module ─────────────────────────────────────────────────
-- tech-debt: guest.hotel_id → hotel.id
ALTER TABLE guest
    ADD COLUMN hotel_id UUID REFERENCES hotel(id);

CREATE INDEX idx_guest_hotel_id ON guest (hotel_id);

-- ── reservation module ───────────────────────────────────────────
-- tech-debt: reservation.hotel_id → hotel.id
ALTER TABLE reservation
    ADD COLUMN hotel_id UUID REFERENCES hotel(id);

CREATE INDEX idx_reservation_hotel_id ON reservation (hotel_id);

-- ── housekeeping module ──────────────────────────────────────────
-- tech-debt: housekeeping_task.hotel_id → hotel.id
ALTER TABLE housekeeping_task
    ADD COLUMN hotel_id UUID REFERENCES hotel(id);

CREATE INDEX idx_hk_task_hotel_id ON housekeeping_task (hotel_id);

-- ── billing module ───────────────────────────────────────────────
-- tech-debt: invoice.hotel_id → hotel.id
ALTER TABLE invoice
    ADD COLUMN hotel_id UUID REFERENCES hotel(id);

CREATE INDEX idx_invoice_hotel_id ON invoice (hotel_id);

-- tech-debt: payment.hotel_id → hotel.id
ALTER TABLE payment
    ADD COLUMN hotel_id UUID REFERENCES hotel(id);

CREATE INDEX idx_payment_hotel_id ON payment (hotel_id);
