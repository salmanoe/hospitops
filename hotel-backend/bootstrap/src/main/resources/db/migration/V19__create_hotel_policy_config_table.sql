-- Phase 9: Hotel policy configuration — tax rate and invoice branding per hotel.
--
-- One row per hotel. Created when a GROUP_ADMIN completes setup wizard Step 2
-- (Policy). Saving a policy config auto-marks the POLICY setup checklist step.
--
-- Billing reads this table (via HotelPolicyPort) at invoice-creation time to
-- determine the tax rate and invoice header/footer content for the hotel.

CREATE TABLE hotel_policy_config (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
    hotel_id            UUID         NOT NULL REFERENCES hotel(id),
    tax_percent         INT          NOT NULL CHECK (tax_percent BETWEEN 0 AND 100),
    tax_name            VARCHAR(50)  NOT NULL,
    invoice_hotel_name  VARCHAR(200) NOT NULL,
    invoice_address     TEXT,
    invoice_footer_note TEXT,
    version             BIGINT       NOT NULL DEFAULT 0,
    created_at          TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT now(),

    CONSTRAINT pk_hotel_policy_config PRIMARY KEY (id),
    CONSTRAINT uq_hotel_policy_config_hotel_id UNIQUE (hotel_id)
);

CREATE INDEX idx_policy_config_hotel_id ON hotel_policy_config (hotel_id);

-- Seed a policy row for the default hotel created in V14 so existing invoices
-- continue to work. The rate mirrors the previously hardcoded PPN_11 constant.
INSERT INTO hotel_policy_config (
    hotel_id, tax_percent, tax_name, invoice_hotel_name,
    invoice_footer_note, version, created_at, updated_at
)
SELECT
    id,
    11,
    'PPN',
    name,
    'Thank you for staying with us. We look forward to welcoming you again.',
    0,
    now(),
    now()
FROM hotel
WHERE id = 'b0000000-0000-0000-0000-000000000002';
