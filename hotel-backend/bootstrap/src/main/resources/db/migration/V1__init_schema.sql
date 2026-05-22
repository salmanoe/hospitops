-- ═══════════════════════════════════════════════════════════════
-- V1__init_schema.sql
-- Place in: bootstrap/src/main/resources/db/migration/
--
-- Design notes:
--   - All PKs are UUIDs (gen_random_uuid()) — safe for future
--     DB-per-service split in Stage 3 microservices.
--   - Cross-module FKs (e.g. RESERVATION.guest_id → GUEST) are
--     enforced here as real FKs (monolith Stage 1).
--     In Stage 3 they become logical references only — the FK
--     constraint is dropped and referential integrity moves to
--     the application/event layer.
--   - Flyway owns all schema changes. JPA ddl-auto=validate only.
-- ═══════════════════════════════════════════════════════════════

-- Enable UUID generation
CREATE
EXTENSION IF NOT EXISTS "pgcrypto";

-- ── ENUMS ─────────────────────────────────────────────────────────
CREATE TYPE staff_role AS ENUM ('ADMIN','MANAGER','FRONT_DESK','HOUSEKEEPING','ACCOUNTANT');
CREATE TYPE room_status AS ENUM ('AVAILABLE','OCCUPIED','DIRTY','MAINTENANCE');
CREATE TYPE reservation_status AS ENUM ('PENDING','CONFIRMED','CHECKED_IN','CHECKED_OUT','CANCELLED');
CREATE TYPE payment_method AS ENUM ('CASH','CREDIT_CARD','DEBIT_CARD','BANK_TRANSFER');
CREATE TYPE payment_status AS ENUM ('UNPAID','PARTIAL','PAID');


-- ═══════════════════════════════════════════════════════════════
-- MODULE: identity
-- Tables: staff
-- Cross-module note: staff.id is referenced by reservation,
--   housekeeping_task, and payment as logical FKs.
--   These are real FKs in Stage 1, dropped in Stage 3.
-- ═══════════════════════════════════════════════════════════════
CREATE TABLE staff
(
    id            UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    full_name     VARCHAR(200) NOT NULL,
    username      VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          staff_role   NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_staff_username ON staff (username);
CREATE INDEX idx_staff_role ON staff (role);


-- ═══════════════════════════════════════════════════════════════
-- MODULE: room
-- Tables: room_type, room, room_rate_override
-- ═══════════════════════════════════════════════════════════════
CREATE TABLE room_type
(
    id          UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    name        VARCHAR(100)   NOT NULL UNIQUE,
    capacity    INT            NOT NULL CHECK (capacity > 0),
    description TEXT,
    base_price  NUMERIC(15, 2) NOT NULL CHECK (base_price >= 0),
    created_at  TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE TABLE room
(
    id           UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    room_number  VARCHAR(10) NOT NULL UNIQUE,
    floor        INT         NOT NULL CHECK (floor > 0),
    status       room_status NOT NULL DEFAULT 'AVAILABLE',
    room_type_id UUID        NOT NULL REFERENCES room_type (id),
    notes        TEXT,
    created_at   TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_room_status ON room (status);
CREATE INDEX idx_room_room_type_id ON room (room_type_id);
CREATE INDEX idx_room_floor ON room (floor);

-- Seasonal / promotional pricing overrides.
-- When a reservation is created, the system checks for an active
-- override for the room_type on the check-in date range.
-- If found, rate_per_night on the reservation uses override price.
-- valid_from and valid_until are inclusive date ranges.
CREATE TABLE room_rate_override
(
    id             UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    room_type_id   UUID           NOT NULL REFERENCES room_type (id),
    name           VARCHAR(100)   NOT NULL, -- e.g. "Weekend Rate", "Lebaran Special"
    price_override NUMERIC(15, 2) NOT NULL CHECK (price_override >= 0),
    valid_from     DATE           NOT NULL,
    valid_until    DATE           NOT NULL,
    created_at     TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_rate_dates CHECK (valid_until >= valid_from)
);

CREATE INDEX idx_rate_override_room_type ON room_rate_override (room_type_id);
CREATE INDEX idx_rate_override_dates ON room_rate_override (valid_from, valid_until);


-- ═══════════════════════════════════════════════════════════════
-- MODULE: guest
-- Tables: guest
-- ═══════════════════════════════════════════════════════════════
CREATE TABLE guest
(
    id          UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    full_name   VARCHAR(200) NOT NULL,
    id_number   VARCHAR(50) UNIQUE, -- passport or national ID
    nationality VARCHAR(100),
    phone       VARCHAR(30),
    email       VARCHAR(150),
    address     TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_guest_full_name ON guest (lower(full_name));
CREATE INDEX idx_guest_id_number ON guest (id_number);
CREATE INDEX idx_guest_email ON guest (lower(email));


-- ═══════════════════════════════════════════════════════════════
-- MODULE: reservation
-- Tables: reservation
--
-- Cross-module FKs (Stage 1 — real constraints):
--   guest_id   → guest.id      (reservation → guest module)
--   room_id    → room.id       (reservation → room module)
--   created_by → staff.id      (reservation → identity module)
--
-- rate_per_night is LOCKED at booking time from room_type.base_price
-- or room_rate_override.price_override. This means billing always
-- uses the agreed rate, even if the room type price changes later.
-- ═══════════════════════════════════════════════════════════════
CREATE TABLE reservation
(
    id                 UUID PRIMARY KEY            DEFAULT gen_random_uuid(),
    reservation_number VARCHAR(20)        NOT NULL UNIQUE,
    guest_id           UUID               NOT NULL REFERENCES guest (id),
    room_id            UUID               NOT NULL REFERENCES room (id),
    check_in_date      DATE               NOT NULL,
    check_out_date     DATE               NOT NULL,
    status             reservation_status NOT NULL DEFAULT 'PENDING',
    -- Rate locked at booking time — immune to future price changes
    rate_per_night     NUMERIC(15, 2)     NOT NULL CHECK (rate_per_night >= 0),
    adults             INT                NOT NULL DEFAULT 1 CHECK (adults > 0),
    children           INT                NOT NULL DEFAULT 0 CHECK (children >= 0),
    special_requests   TEXT,
    -- Logical FK to identity module (staff who created the reservation)
    created_by         UUID               REFERENCES staff (id) ON DELETE SET NULL,
    created_at         TIMESTAMP          NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP          NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_reservation_dates CHECK (check_out_date > check_in_date)
);

CREATE INDEX idx_reservation_number ON reservation (reservation_number);
CREATE INDEX idx_reservation_guest_id ON reservation (guest_id);
CREATE INDEX idx_reservation_room_id ON reservation (room_id);
CREATE INDEX idx_reservation_status ON reservation (status);
CREATE INDEX idx_reservation_check_in_date ON reservation (check_in_date);
CREATE INDEX idx_reservation_check_out_date ON reservation (check_out_date);
-- Composite: availability query — most frequent query in the system
CREATE INDEX idx_reservation_availability ON reservation (room_id, check_in_date, check_out_date, status);

-- Auto-generate reservation_number: RES-YYYY-NNNNN
-- Called by application layer before insert.
CREATE SEQUENCE reservation_number_seq START 1;

CREATE
OR REPLACE FUNCTION generate_reservation_number()
RETURNS TEXT AS $$
BEGIN
RETURN 'RES-' || TO_CHAR(NOW(), 'YYYY') || '-' ||
       LPAD(nextval('reservation_number_seq')::TEXT, 5, '0');
END;
$$
LANGUAGE plpgsql;


-- ═══════════════════════════════════════════════════════════════
-- MODULE: housekeeping
-- Tables: housekeeping_task
--
-- Cross-module FKs:
--   room_id        → room.id        (housekeeping → room)
--   reservation_id → reservation.id (housekeeping → reservation)
--                    NULLABLE — tasks can be created independently
--                    of a reservation (e.g. deep cleaning, maintenance)
--   assigned_to    → staff.id       (housekeeping → identity)
-- ═══════════════════════════════════════════════════════════════
CREATE TABLE housekeeping_task
(
    id             UUID PRIMARY KEY   DEFAULT gen_random_uuid(),
    room_id        UUID      NOT NULL REFERENCES room (id),
    -- Optional: links task to the checkout that triggered it
    reservation_id UUID      REFERENCES reservation (id) ON DELETE SET NULL,
    -- Optional: which staff member is responsible
    assigned_to    UUID      REFERENCES staff (id) ON DELETE SET NULL,
    notes          TEXT,
    completed      BOOLEAN   NOT NULL DEFAULT FALSE,
    completed_at   TIMESTAMP,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_hk_task_room_id ON housekeeping_task (room_id);
CREATE INDEX idx_hk_task_reservation_id ON housekeeping_task (reservation_id);
CREATE INDEX idx_hk_task_assigned_to ON housekeeping_task (assigned_to);
CREATE INDEX idx_hk_task_completed ON housekeeping_task (completed);


-- ═══════════════════════════════════════════════════════════════
-- MODULE: billing
-- Tables: invoice, invoice_item, payment
--
-- Cross-module FKs:
--   reservation_id → reservation.id (billing → reservation)
--   received_by    → staff.id       (billing → identity)
--
-- invoice_number is human-readable: INV-YYYY-NNNNN
-- due_date supports accounts-receivable tracking for
--   corporate or deferred payment scenarios.
-- ═══════════════════════════════════════════════════════════════
CREATE TABLE invoice
(
    id              UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    invoice_number  VARCHAR(20)    NOT NULL UNIQUE,
    reservation_id  UUID           NOT NULL UNIQUE REFERENCES reservation (id),
    subtotal        NUMERIC(15, 2) NOT NULL DEFAULT 0 CHECK (subtotal >= 0),
    tax_amount      NUMERIC(15, 2) NOT NULL DEFAULT 0 CHECK (tax_amount >= 0),
    discount_amount NUMERIC(15, 2) NOT NULL DEFAULT 0 CHECK (discount_amount >= 0),
    total_amount    NUMERIC(15, 2) NOT NULL DEFAULT 0 CHECK (total_amount >= 0),
    payment_status  payment_status NOT NULL DEFAULT 'UNPAID',
    due_date        DATE, -- NULL = payable immediately (walk-in)
    notes           TEXT,
    issued_at       TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_invoice_reservation_id ON invoice (reservation_id);
CREATE INDEX idx_invoice_payment_status ON invoice (payment_status);
CREATE INDEX idx_invoice_due_date ON invoice (due_date);
CREATE INDEX idx_invoice_number ON invoice (invoice_number);

-- Auto-generate invoice_number: INV-YYYY-NNNNN
CREATE SEQUENCE invoice_number_seq START 1;

CREATE
OR REPLACE FUNCTION generate_invoice_number()
RETURNS TEXT AS $$
BEGIN
RETURN 'INV-' || TO_CHAR(NOW(), 'YYYY') || '-' ||
       LPAD(nextval('invoice_number_seq')::TEXT, 5, '0');
END;
$$
LANGUAGE plpgsql;

CREATE TABLE invoice_item
(
    id          UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    invoice_id  UUID           NOT NULL REFERENCES invoice (id) ON DELETE CASCADE,
    description VARCHAR(255)   NOT NULL,
    quantity    INT            NOT NULL DEFAULT 1 CHECK (quantity > 0),
    unit_price  NUMERIC(15, 2) NOT NULL CHECK (unit_price >= 0),
    total_price NUMERIC(15, 2) NOT NULL CHECK (total_price >= 0)
);

CREATE INDEX idx_invoice_item_invoice_id ON invoice_item (invoice_id);

CREATE TABLE payment
(
    id           UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    invoice_id   UUID           NOT NULL REFERENCES invoice (id),
    amount       NUMERIC(15, 2) NOT NULL CHECK (amount > 0),
    method       payment_method NOT NULL,
    reference_no VARCHAR(100), -- card last 4, transfer ref, etc.
    paid_at      TIMESTAMP      NOT NULL DEFAULT NOW(),
    -- Logical FK to identity module
    received_by  UUID           REFERENCES staff (id) ON DELETE SET NULL
);

CREATE INDEX idx_payment_invoice_id ON payment (invoice_id);
CREATE INDEX idx_payment_paid_at ON payment (paid_at);
CREATE INDEX idx_payment_method ON payment (method);
